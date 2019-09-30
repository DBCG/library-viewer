package com.alphora.cql;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IFetchConformanceUntyped;

public class ViewerController {

    public ViewerController() {
        this.currentContext = FhirContext.forDstu3();
    }

    private FhirContext currentContext;

    @FXML
    Button get;

    @FXML
    Button put;

    @FXML
    TextArea cql;

    @FXML
    TextField url;

    @FXML
    TextField status;

    @FXML
    private void get() throws IOException {
        try {
            String url = this.url.getText();
            IGenericClient client = this.getClient(url);
            String result = null;
            if (this.currentContext.getVersion().getVersion() == FhirVersionEnum.DSTU3) {
                result = this.get3(client, url);
            }
            else {
                result = this.get4(client, url);
            }

            if (result == null || result.isEmpty()) {
                this.cql.setText("");
                this.status.setText("No CQL Found for this Library.");
            }
            else {
                this.status.setText("Library Loaded");
                this.cql.setText(result);
            }
        }
        catch (URISyntaxException e) {
            this.status.setText("Invalid / Malformed URL");
        }
        catch (Exception e) {
            this.status.setText(e.getMessage());
        }
    }

    @FXML
    private void put() throws IOException {
        try {
            String url = this.url.getText();
            IGenericClient client = this.getClient(url);
            if (this.currentContext.getVersion().getVersion() == FhirVersionEnum.DSTU3) {
                this.put3(client, url);
            }
            else {
                this.put4(client, url);
            }

            this.status.setText("Library Updated");
        }
        catch (URISyntaxException e) {
            this.status.setText("Invalid / Malformed URL");
        }
        catch (Exception e) {
            this.status.setText(e.getMessage());
        }
    }


    private URI getFhirBaseUri(URI uri) {
        // TODO: This should actually just recognize the FHIR types.
        int index = uri.getPath().lastIndexOf("Library/");
        if (index > -1) {
            uri = getHead(uri);
        }

        index = uri.getPath().lastIndexOf("/Library");
        if (index > -1) {
            uri = getHead(uri);
        }

        return uri;
    }

    private URI getHead(URI uri) {
        String path = uri.getPath();
        if (path != null) {
            int index = path.lastIndexOf("/");
            if (index > -1) {
                return withPath(uri, path.substring(0, index));
            }

            return uri;
        }

        return uri;
    }

    private  URI withPath(URI uri, String path) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, uri.getFragment(), uri.getQuery());
        }
        catch (Exception e) {
            return null;
        }
    }

    private void setupContext(String baseUrl){
        this.setContext(this.getVersion(baseUrl));
    }

    private FhirVersionEnum getVersion(String baseUrl) {
        IBaseConformance c = this.currentContext.newRestfulGenericClient(baseUrl).capabilities().ofType(IBaseConformance.class).execute();
        return c.getStructureFhirVersionEnum();
    }

    private void setContext(FhirVersionEnum version) {
        if (this.currentContext.getVersion().getVersion().equals(version)) {
            return;
        }
        else {
            this.currentContext = version.newContext();
        }
    }

    private  IGenericClient getClient(String url) throws URISyntaxException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("No URL entered");
        }

        URI parsed = new URI(url);
        String baseUrl = this.getFhirBaseUri(parsed).toString();

        this.setupContext(baseUrl);
        return this.currentContext.newRestfulGenericClient(baseUrl);
    }

    private void put3(IGenericClient client, String url) {
        Library resource = client.read().resource(Library.class).withUrl(url).execute();

        resource.setContent(
            resource.getContent().stream().filter(x -> !x.getContentType().equals("text/cql")).collect(Collectors.toList())
        );

        Attachment attachment = new Attachment();
        attachment.setContentType("text/cql");
        attachment.setData(this.cql.getText().getBytes());

        resource.getContent().add(attachment);

        client.update().resource(resource).execute();
    }

    private void put4(IGenericClient client, String url) {
        org.hl7.fhir.r4.model.Library resource = client.read().resource(org.hl7.fhir.r4.model.Library.class).withUrl(url).execute();

        resource.setContent(
            resource.getContent().stream().filter(x -> !x.getContentType().equals("text/cql")).collect(Collectors.toList())
        );

        org.hl7.fhir.r4.model.Attachment attachment = new org.hl7.fhir.r4.model.Attachment();
        attachment.setContentType("text/cql");
        attachment.setData(this.cql.getText().getBytes());

        resource.getContent().add(attachment);

        client.update().resource(resource).execute();
    }

    private String get3(IGenericClient client, String url) {
        Library resource = client.read().resource(Library.class).withUrl(url).execute();
        Optional<Attachment> attachment = resource.getContent().stream().filter(x -> x.getContentType().equals("text/cql")).findFirst();
        if (attachment.isPresent())
        {
            return new String(attachment.get().getData(), StandardCharsets.UTF_8); 
        }
        
        return null;
    }

    private String get4(IGenericClient client, String url) {
        org.hl7.fhir.r4.model.Library resource = client.read().resource(org.hl7.fhir.r4.model.Library.class).withUrl(url).execute();
        Optional< org.hl7.fhir.r4.model.Attachment> attachment = resource.getContent().stream().filter(x -> x.getContentType().equals("text/cql")).findFirst();
        if (!attachment.isPresent())
        {
            return new String(attachment.get().getData(), StandardCharsets.UTF_8);
        }

        return null;
    }
}
