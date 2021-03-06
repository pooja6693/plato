/*******************************************************************************
 * Copyright 2006 - 2012 Vienna University of Technology,
 * Department of Software Technology and Interactive Systems, IFS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.scape_project.planning.application;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;

import eu.scape_project.planning.exception.PlanningException;
import eu.scape_project.planning.manager.PlanManager;
import eu.scape_project.planning.model.Alternative;
import eu.scape_project.planning.model.Plan;
import eu.scape_project.planning.model.PlanProperties;
import eu.scape_project.planning.model.PlatoException;
import eu.scape_project.planning.model.User;
import eu.scape_project.planning.utils.MemoryTest;
import eu.scape_project.planning.utils.OS;
import eu.scape_project.planning.xml.PlanXMLConstants;
import eu.scape_project.planning.xml.ProjectExportAction;
import eu.scape_project.planning.xml.ProjectExporter;
import eu.scape_project.planning.xml.ProjectImporter;

@Stateless
public class AdminActions implements Serializable {
    private static final long serialVersionUID = -5811809194521269245L;

    @Inject
    private Logger log;

    @Inject
    private EntityManager em;

    @Inject
    private User user;

    @Inject
    private ProjectExportAction projectExportAction;

    @Inject
    private ProjectImporter projectImporter;

    @Inject
    private PlanManager planManager;

    @Inject
    private MemoryTest memoryTest;

    /**
     * Predefined hash coded passcode computed by SHA-1
     */
    private String adminPasscode = "responsibility";

    /**
     * Method responsible for checking if the given admin-password is correct.
     * 
     * @param password
     *            Password to check for correctness.
     * @return True if the password is correct, false otherwise.
     */
    public boolean isAdminPasswordCorrect(String password) {
        return adminPasscode.equals(password);
    }

    /**
     * Exports all plans into a single xml file.
     * 
     * @return True if export was successful, false otherwise.
     */
    public boolean exportAllPlansToZip() {
        return projectExportAction.exportAllProjectsToZip();
    }

    /**
     * Exports all plans with planproperty-ids between fromPlanPropertiesId and
     * toPlanProperitesId into a single xml file.
     * 
     * @param fromPlanPropertiesId
     *            Start of the id range to export.
     * @param toPlanProperitesId
     *            End of the id range to export.
     * @return True if export was successful, false otherwise.
     */
    public boolean exportSomePlansToZip(int fromPlanPropertiesId, int toPlanProperitesId) {
        return projectExportAction.exportSomeProjectsToZip(fromPlanPropertiesId, toPlanProperitesId);
    }

    /**
     * Method responsible for retrieving the path where the last project export
     * was put into.
     * 
     * @return Path where the last project export was put into, or null if no
     *         project export was done in this session yet.
     */
    public String getLastProjectExportPath() {
        return projectExportAction.getLastProjectExportPath();
    }

    /**
     * Method responsible for deleting all plans from database.
     */
    
    public boolean deleteAllPlans() {
        @SuppressWarnings("unchecked")
        List<Plan> planList = em.createQuery("select p from Plan p").getResultList();
        boolean gotError = false;
        for (Plan p : planList) {
            try {
                planManager.deletePlan(p);
            } catch (PlanningException e) {
                // just log the error, and try to delete the other plans
                gotError = true;
                log.error(e.getMessage(), e);
            }
        }
        em.flush();
        return !gotError;
    }

    /**
     * Method responsible for cleaning-up/removing all loose Plan Values.
     * 
     * @return Number of cleaned-up/removed Values objects.
     */
    public int cleanUpLoosePlanValues() {
        @SuppressWarnings("unchecked")
        List<PlanProperties> ppList = em.createQuery("select p from PlanProperties p").getResultList();
        int total = 0;
        int i = 0;

        for (PlanProperties pp : ppList) {
            int number = cleanupProject(pp.getId());
            log.info("Plan " + pp.getName() + ": removed " + number + " values.");
            total += number;
            i++;
            if ((i % 5) == 0) {
                System.gc();
            }
        }

        return total;
    }

    /**
     * Method responsible for unlocking a specific plan.
     * 
     * @param planPropertiesId
     *            PlanProperties-id of the plan to unlock.
     * @return True if unlocking was successful, false otherwise.
     */
    public boolean unlockPlan(int planPropertiesId) {
        Query q = em.createQuery("update PlanProperties pp set pp.openHandle = 0 where pp.id = " + planPropertiesId);

        if (q.executeUpdate() < 1) {
            log.info("Unlocking project with PlanPropertiesId " + planPropertiesId + " failed.");
            return false;
        } else {
            log.info("Unlocked project with PlanPropertiesId " + planPropertiesId);
            return true;
        }
    }

    /**
     * Method responsible for cloning a specific plan.
     * 
     * @param planPropertiesId
     *            PlanProperties-id of the plan to clone.
     * @return True if cloning was successful, false otherwise.
     */
    public boolean clonePlan(Integer planPropertiesId) {
        Plan selectedPlan;
        try {
            selectedPlan = (Plan)em.createQuery("select p from Plan p where p.planProperties.id = " + planPropertiesId).getSingleResult();
            File tempFile = new File(OS.getTmpPath() + "cloneplans_" + System.currentTimeMillis() + ".xml");
            tempFile.deleteOnExit();
            ProjectExporter exporter = new ProjectExporter();
            
            boolean success = false;
            
            try {
                exporter.exportToFile(selectedPlan, tempFile);
                List<Plan> plans = projectImporter.importPlans(new FileInputStream(tempFile));
                // store project
                storePlans(plans);
                success = true;
                log.debug("Plan '" + selectedPlan.getPlanProperties().getName() + "' successfully cloned.");
            } catch (Exception e) {
                log.error("Could not clone project: '" + selectedPlan.getPlanProperties().getName() + "'.", e);
            }
            
            tempFile.delete();
        
            return success;

        } catch (Exception e1) {
            log.error("Failed to retrieve plan for cloning: " + planPropertiesId);
        }
        return false;

    }

    /**
     * Method responsible for deleting a specific plan.
     * 
     * @param planPropertiesId
     *            PlanPropertiesId of the plan to delete.
     * @return True if deletion was successful, false otherwise.
     */
    public boolean deletePlan(int planPropertiesId)  {
        try {
            Plan plan = (Plan)em.createQuery("select p from Plan p where p.planProperties.id = " + planPropertiesId).getSingleResult();
            planManager.deletePlan(plan);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Method responsible for unlocking all locked plans.
     */
    public void unlockAllPlans() {
        planManager.unlockAll();
    }

    /**
     * Method responsible for throwing a RuntimeException.
     */
    public void throwRuntimeException() {
        throw new RuntimeException("AdminUtils Test-Exception");
    }

    /**
     * Method responsible for munching memory.
     * 
     * @param mb
     *            Memory to munch (in MB).
     */
    public void munchMem(int mb) {
        log.info("Munching " + mb + " MB of memory.");
        memoryTest.munchMem(mb);
    }

    /**
     * Method responsible for releasing memory.
     */
    public void releaseMem() {
        log.info("Releasing memory...");
        memoryTest.releaseMem();
    }

    /**
     * Method responsible for importing Plans from a given directory.
     * 
     * @param directory
     *            Import directory
     * @return Number of plans imported successfully.
     */
    public int importPlansFromDirectory(String directory) {
        int count = 0;

        try {
            count = projectImporter.importAllProjectsFromDir(directory);
        } catch (PlatoException e) {
            log.error("failed to import plans from " + directory, e);
            return 0;
        }

        return count;
    }

    /**
     * Method responsible for importing Plans from a given file.
     * 
     * @param file
     *            File which contains plan in xml-format.
     * @param changeUser
     *            Indicates if the imported plan should be assigned to the
     *            current user (thus be imported for only this user)
     * @return Number of plans imported successfully.
     */
    public int importPlansFromFile(byte[] fileData, boolean changeUser) {
        // check input
        if (fileData == null || fileData.length == 0) {
            log.error("Invalid file passed for import.");
            return 0;
        }

        log.debug("Try to import plans from file");

        int nrOrPlans = 0;
        List<Plan> plansToImport = new ArrayList<Plan>();

        // start import
        try {
            plansToImport = projectImporter.importPlans(new ByteArrayInputStream(fileData));
            nrOrPlans = plansToImport.size();
        } catch (Exception e) {
            log.error("failed to import plans from file.", e);
            return 0;
        }

        // if the plans are imported by a NORMAL USER in the web interface, they
        // will be
        // assigned to this user, i.e. the owner is set to the current user.
        // If they are imported by an ADMIN, they stay property of the original
        // user,
        // unless the admin uses a different button
        if (!user.isAdmin() || changeUser) {
            for (Plan p : plansToImport) {
                p.getPlanProperties().setOwner(user.getUsername());
            }
        }

        // store plans
        storePlans(plansToImport);

        return nrOrPlans;
    }

    /**
     * Method responsible for importing plans via their given xml
     * representation.
     * 
     * @param xml
     *            Xml representation of the plans.
     * @return Number of plans imported successfully.
     */
    public int importPlansFromXml(String xml) {
        int importedPlans = 0;
        List<Plan> plansToImport = new ArrayList<Plan>();

        try {
            plansToImport = projectImporter.importPlans(new ByteArrayInputStream(xml.getBytes(PlanXMLConstants.ENCODING)));
        } catch (Exception e) {
            log.error("failed to import plans from xml.", e);
            return 0;
        }

        importedPlans = plansToImport.size();
        storePlans(plansToImport);

        return importedPlans;
    }

    /**
     * Method responsible for cleaning-up/removing loose Plan Values for a given
     * Plan.
     * 
     * @param pid
     *            PlanProperties id of the Plan to operate on.
     * @return Number of cleanedUp/removed Values objects.
     */
    private int cleanupProject(int pid) {
        try {
            Plan p = (Plan)em.createQuery("select p from Plan p where p.planProperties.id = " + pid).getSingleResult();
            List<String> alternativeNames = new ArrayList<String>();
            
            for (Alternative a : p.getAlternativesDefinition().getAlternatives()) {
                alternativeNames.add(a.getName());
            }
            
            int number = p.getTree()
                .removeLooseValues(alternativeNames, p.getSampleRecordsDefinition().getRecords().size());
            log.info("cleaned up values for plan " + p.getPlanProperties().getName() + " - removed " + number + " Value(s) instances.");
            
            if (number > 0) {
                em.persist(p.getTree());
            }
            em.clear();
            
            return number;
        } catch (Exception e) {
            log.error("Failed to retrieve plan for clean-up. id: " + pid, e);
            return 0;
        }
    }

    /**
     * Method responsible for storing plans in database.
     * 
     * @param plans
     *            Plans to store.
     */
    private void storePlans(List<Plan> plans) {
        while (!plans.isEmpty()) {
            Plan plan = plans.get(0);
            em.persist(plan);
            em.flush();

            plans.remove(plan);
            plan = null;
            em.clear();
            System.gc();
        }
    }
}
