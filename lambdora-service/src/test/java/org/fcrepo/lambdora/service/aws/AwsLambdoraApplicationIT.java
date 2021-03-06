package org.fcrepo.lambdora.service.aws;

import org.fcrepo.lambdora.common.test.IntegrationTestBase;
import org.fcrepo.lambdora.service.api.BinaryService;
import org.fcrepo.lambdora.service.api.Container;
import org.fcrepo.lambdora.service.api.ContainerService;
import org.fcrepo.lambdora.service.api.LambdoraApplication;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.CONTAINER;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.CREATED_DATE;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.HAS_PARENT;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.INTERNAL_URI_PREFIX;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.lambdora.common.rdf.RdfLexicon.TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * AwsLambdoraApplicationIT - Integration test for the LambdoraApplication
 * DI framework.
 *
 * @author dbernstein
 */
public class AwsLambdoraApplicationIT extends IntegrationTestBase {

    private LambdoraApplication application;

    @Override
    public void setup() {
        super.setup();

        application = DaggerAwsLambdoraApplication.builder()
            .awsServiceModule(new AwsServiceModule(this.dynamodbClient))
            .build();
    }

    /**
     * A simple smoke test.
     */
    @Test
    public void smokeTest() {

        final ContainerService containerService = application.containerService();
        assertNotNull(containerService);
        final BinaryService binaryService = application.binaryService();
        assertNotNull(binaryService);
    }

    /**
     * Tests creation, existence and retrieval of a new container
     */
    @Test
    public void testContainerServiceRoundTrip() {
        final ContainerService containerService = application.containerService();
        final URI identifier = URI.create(INTERNAL_URI_PREFIX + "/test");
        final URI parent = URI.create(INTERNAL_URI_PREFIX + "/");

        assertFalse("identifier should not exist", containerService.exists(identifier));
        final Container container = containerService.findOrCreate(identifier);
        assertEquals("identifiers are equal", identifier, container.getIdentifier());
        final AtomicInteger count = new AtomicInteger(0);

        // Check for all system-generated triples for a Container
        assertTrue("object is ldp:Container",
            container.getTriples().anyMatch(triple -> triple.getSubject().getURI().equals(identifier.toString()) &&
                    triple.getPredicate().getURI().equals(TYPE.getURI()) &&
                    triple.getObject().getURI().equals(CONTAINER.getURI())
            ));

        assertTrue("object is ldp#RDFSource",
            container.getTriples().anyMatch(triple -> triple.getSubject().getURI().equals(identifier.toString()) &&
                triple.getPredicate().getURI().equals(TYPE.getURI()) &&
                triple.getObject().getURI().equals(RDF_SOURCE.getURI())
            ));

        assertTrue("object is fedora#Container",
            container.getTriples().anyMatch(triple -> triple.getSubject().getURI().equals(identifier.toString()) &&
                triple.getPredicate().getURI().equals(TYPE.getURI()) &&
                triple.getObject().getURI().equals(FEDORA_CONTAINER.getURI())
            ));

        assertTrue("object is fedora#Resource",
            container.getTriples().anyMatch(triple -> triple.getSubject().getURI().equals(identifier.toString()) &&
                triple.getPredicate().getURI().equals(TYPE.getURI()) &&
                triple.getObject().getURI().equals(FEDORA_RESOURCE.getURI())
            ));

        assertTrue("object has fedora#created date",
            container.getTriples().anyMatch(triple -> triple.getSubject().getURI().equals(identifier.toString()) &&
                triple.getPredicate().getURI().equals(CREATED_DATE.getURI())
            ));

        assertTrue("object has fedora#hasParent",
            container.getTriples().anyMatch(triple -> triple.getSubject().getURI().equals(identifier.toString()) &&
                triple.getPredicate().getURI().equals(HAS_PARENT.getURI()) &&
                triple.getObject().getURI().equals(parent.toString())
            ));

        assertTrue("identifier should exist", containerService.exists(identifier));
    }
}
