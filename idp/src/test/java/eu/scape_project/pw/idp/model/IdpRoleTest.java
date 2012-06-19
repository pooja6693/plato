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
package eu.scape_project.pw.idp.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class IdpRoleTest {
    private static EntityManagerFactory emFactory;
    private static EntityManager em;

    @BeforeClass
    public static void oneTimeSetUp() {
        emFactory = Persistence.createEntityManagerFactory("platodbTest");
        em = emFactory.createEntityManager();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        em.close();
        emFactory.close();
    }

    @Test
    public void getUserWorksAsExpected() throws Exception {
        // ----- set-up -----
        deleteUserIfExistent("user1");
        deleteUserIfExistent("user2");
        deleteRoleIfExistent("adminrole");
        deleteRoleIfExistent("managerrole");

        IdpRole adminRole = new IdpRole();
        adminRole.setRoleName("adminrole");
        IdpRole managerRole = new IdpRole();
        managerRole.setRoleName("managerrole");

        IdpUser user1 = createUser("user1");
        user1.getRoles().add(managerRole);

        IdpUser user2 = createUser("user2");
        user2.getRoles().add(adminRole);
        user2.getRoles().add(managerRole);

        em.getTransaction().begin();
        em.persist(user1);
        em.persist(user2);
        em.getTransaction().commit();

        // ----- test -----

        em.refresh(adminRole);
        em.refresh(managerRole);

        assertEquals(1, adminRole.getUser().size());
        assertEquals(2, managerRole.getUser().size());
        assertTrue(adminRole.getUser().contains(user2));
        assertTrue(managerRole.getUser().contains(user1));
        assertTrue(managerRole.getUser().contains(user2));
    }

    @Test
    public void deleteUnassignedRole_works() throws Exception {
        // ----- set-up -----
        IdpRole unassignedRole = new IdpRole();
        unassignedRole.setRoleName("unassignedrole");

        em.getTransaction().begin();
        em.persist(unassignedRole);
        em.getTransaction().commit();

        // ----- test -----

        em.getTransaction().begin();
        em.remove(unassignedRole);
        em.getTransaction().commit();

        // if I reach this line - test succeeds
        assertTrue(true);
    }

    @Test(expected = Exception.class)
    public void deleteAssignedRole_fails() throws Exception {
        // ----- set-up -----
        deleteUserIfExistent("user1");
        deleteRoleIfExistent("assignedrole");

        IdpRole assignedRole = new IdpRole();
        assignedRole.setRoleName("assignedrole");

        IdpUser user1 = createUser("user1");
        user1.getRoles().add(assignedRole);

        em.getTransaction().begin();
        em.persist(user1);
        em.getTransaction().commit();

        // ----- test -----

        em.getTransaction().begin();
        em.remove(assignedRole);
        em.getTransaction().commit();
    }

    private IdpUser createUser(String username) {
        IdpUser user = new IdpUser();
        user.setUsername(username);
        user.setPlainPassword("mypass");
        user.setFirstName("Max");
        user.setLastName("Mustermann");
        user.setEmail("max@mustermann.at");
        user.setStatus(IdpUserState.CREATED);
        user.setActionToken("uid-123-uid-456");

        return user;
    }

    private void deleteUserIfExistent(String username) {
        // delete probably conflicting test-user
        em.getTransaction().begin();

        List<IdpUser> userToDelete = (List<IdpUser>) em
            .createQuery("SELECT u FROM IdpUser u WHERE u.username = :username").setParameter("username", username)
            .getResultList();

        for (IdpUser delUser : userToDelete) {
            em.remove(delUser);
        }

        em.getTransaction().commit();
    }

    private void deleteRoleIfExistent(String roleName) {
        // delete probably conflicting test-user
        em.getTransaction().begin();

        List<IdpRole> rolesToDelete = (List<IdpRole>) em
            .createQuery("SELECT r FROM IdpRole r WHERE r.roleName = :roleName").setParameter("roleName", roleName)
            .getResultList();

        for (IdpRole delRole : rolesToDelete) {
            em.remove(delRole);
        }

        em.getTransaction().commit();
    }
}