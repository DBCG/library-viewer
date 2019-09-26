package com.alphora.cql;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class ViewerController {

    public ViewerController() {
        this.fhirContext = FhirContext.forDstu3();
    }

    
    private FhirContext fhirContext;

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
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("No URL entered");
            }

            URI parsed = new URI(url);

            Library resource = this.fhirContext.newRestfulGenericClient(this.getFhirBaseUri(parsed).toString())
                .read().resource(Library.class).withUrl(parsed.toString()).execute();

            Optional<Attachment> attachment = resource.getContent().stream().filter(x -> x.getContentType().equals("text/cql")).findFirst();

            if (!attachment.isPresent())
            {
                this.cql.setText("");
                this.status.setText("No CQL Found for this Library.");
            }
            else {
                this.status.setText("Library Loaded");
                this.cql.setText(new String(attachment.get().getData(), StandardCharsets.UTF_8));
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
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("No URL entered");
            }

            URI parsed = new URI(url);

            IGenericClient client = this.fhirContext.newRestfulGenericClient(
                this.getFhirBaseUri(parsed).toString()
            );
            
            
            Library resource = client.read().resource(Library.class).withUrl(parsed.toString()).execute();

            resource.setContent(
                resource.getContent().stream().filter(x -> !x.getContentType().equals("text/cql")).collect(Collectors.toList())
            );

            Attachment attachment = new Attachment();
            attachment.setContentType("text/cql");
            attachment.setData(this.cql.getText().getBytes());

            resource.getContent().add(attachment);

            client.update().resource(resource).execute();
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
}
