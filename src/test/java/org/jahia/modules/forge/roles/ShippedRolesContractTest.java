package org.jahia.modules.forge.roles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the permissions of each role the module ships in {@code src/main/import/roles.xml}.
 *
 * <p>A role's {@code j:permissionNames} is a site-scoped grant: it applies to the whole site subtree
 * of every site the role is granted on, not only to the nodes the holder created. It is data rather
 * than code, so no call path guards it and a review of the Java alone never sees a change to it.
 * This test states what each role is meant to carry, so adding or removing a permission is a
 * deliberate edit to an expectation and not a silent widening.
 *
 * <p>A role grants along two axes, and the test states both. The role element's own
 * {@code j:permissionNames} is the site-scoped grant. A nested {@code jnt:externalPermissions} child
 * grants further permissions on the path it names, so a role can be widened without its own attribute
 * changing at all.
 *
 * <p>The file is read from the source tree because the build packs it into
 * {@code META-INF/import.zip} rather than onto the classpath.
 */
class ShippedRolesContractTest {

    private static final Path ROLES_XML = Paths.get("src", "main", "import", "roles.xml");
    private static final String PERMISSION_NAMES = "j:permissionNames";
    private static final String PRIMARY_TYPE = "jcr:primaryType";
    private static final String EXTERNAL_PERMISSIONS = "jnt:externalPermissions";

    /**
     * The permissions a store developer needs to publish a module into the store, and no others.
     * {@code jahiaForgeUploadModule} authorises the upload action itself and {@code jcr:write_live}
     * covers writing the module entry. The owner role that the upload grants on each created node is
     * written by the module through a system session, so the uploader's own role needs no
     * access-control rights.
     */
    private static final Set<String> STORE_DEVELOPER = permissions("jahiaForgeUploadModule", "jcr:write_live");

    /** The administrator tier, which the developer tier must never reach. */
    private static final Set<String> STORE_ADMINISTRATOR = permissions(
            "jahiaForgeModerateModule", "jahiaForgeUploadModule", "jcr:all_live", "jcr:all_default");

    /** The store administrator reaches the shared category tree, and the store developer reaches nothing. */
    private static final Set<String> STORE_ADMINISTRATOR_EXTERNAL =
            permissions("/sites/systemsite/categories => jcr:all_default");

    private static Document document;

    @BeforeAll
    static void parseRolesXml() throws Exception {
        assertThat(Files.isRegularFile(ROLES_XML)).as("%s is readable from the module directory", ROLES_XML).isTrue();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // A DOCTYPE is the entry point for an entity expansion, and roles.xml has none.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        document = factory.newDocumentBuilder().parse(ROLES_XML.toFile());
    }

    private static Set<String> permissions(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    @Test
    @DisplayName("store-developer grants exactly the permissions a module upload needs")
    void storeDeveloperPermissions() {
        assertThat(grantedBy("store-developer")).isEqualTo(STORE_DEVELOPER);
    }

    @Test
    @DisplayName("store-developer grants nothing on a path outside its own site scope")
    void storeDeveloperExternalPermissions() {
        assertThat(externalGrantsOf("store-developer")).isEmpty();
    }

    @Test
    @DisplayName("store-administrator keeps its own permission set")
    void storeAdministratorPermissions() {
        assertThat(grantedBy("store-administrator")).isEqualTo(STORE_ADMINISTRATOR);
    }

    @Test
    @DisplayName("store-administrator keeps its one external grant, on the shared category tree")
    void storeAdministratorExternalPermissions() {
        assertThat(externalGrantsOf("store-administrator")).isEqualTo(STORE_ADMINISTRATOR_EXTERNAL);
    }

    @Test
    @DisplayName("the file ships these two roles and no third one")
    void rolesShipped() {
        assertThat(roleNames()).containsExactlyInAnyOrder("store-developer", "store-administrator");
    }

    /** The permissions the named role grants at site scope. */
    private static Set<String> grantedBy(String roleName) {
        return permissions(attribute(role(roleName), PERMISSION_NAMES).split("\\s+"));
    }

    /**
     * Every grant the named role makes on another path, as {@code <path> => <permissions>}. A
     * {@code jnt:externalPermissions} child widens a role without touching its own attribute, so a
     * test that reads only {@code j:permissionNames} would not see it.
     */
    private static Set<String> externalGrantsOf(String roleName) {
        Set<String> grants = new TreeSet<>();
        for (Element node : descendantsOf(role(roleName))) {
            if (EXTERNAL_PERMISSIONS.equals(attribute(node, PRIMARY_TYPE))) {
                grants.add(attribute(node, "j:path") + " => " + attribute(node, PERMISSION_NAMES));
            }
        }
        return grants;
    }

    private static Element role(String roleName) {
        for (Element child : childElementsOf(document.getDocumentElement())) {
            if (roleName.equals(child.getNodeName())) {
                return child;
            }
        }
        throw new AssertionError("role " + roleName + " is not declared in " + ROLES_XML);
    }

    private static Set<String> roleNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Element child : childElementsOf(document.getDocumentElement())) {
            names.add(child.getNodeName());
        }
        return names;
    }

    /** Every element under {@code parent}, at any depth. */
    private static List<Element> descendantsOf(Element parent) {
        List<Element> found = new ArrayList<>();
        for (Element child : childElementsOf(parent)) {
            found.add(child);
            found.addAll(descendantsOf(child));
        }
        return found;
    }

    private static List<Element> childElementsOf(Element parent) {
        List<Element> found = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                found.add((Element) child);
            }
        }
        return found;
    }

    private static String attribute(Element element, String name) {
        return element.getAttribute(name).trim();
    }
}
