package Crawler;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class XhtmlConverter {

    String url ;

    public XhtmlConverter(String url) {
        this.url = url;
    }

    public String convertUrl() throws IOException {

            InputStream inputStream = new URL(url).openStream();

            Tidy tidy = new Tidy();
            tidy.setXHTML(true);
            tidy.setShowWarnings(false);
            tidy.setQuiet(true);

            Document xhtmlDocument = tidy.parseDOM(inputStream, null);

            OutputStream outputStream = new ByteArrayOutputStream();
            tidy.pprint(xhtmlDocument, outputStream);

            String xhtmlContent = outputStream.toString();
            return (xhtmlContent);

    }

}


