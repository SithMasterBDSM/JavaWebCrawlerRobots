//===========================================================================

package webcrawler;

// Java classes
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.util.List;

//
        
//
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.json.JSONObject;

import vsdk.toolkit.common.VSDK;

/**
This class is meant to keep an on-memory copy of a HTML or similar file.
The stored data structure keeps some structure to give hints over the
original HTML type data:
  - HTML bytes are partitioned in different areas: inside tag, or between
    tags areas
  - For each tag, tag name and tag parameters can be retrieved
  - Some tag search/query operations are provided over the structure
  - Original data can be reconstructed from data structure's copy
*/
public class TaggedHtml
{
    private static final int OUTSIDE_TAG = 1;
    private static final int INSIDE_TAG = 2;
    private int currentState = OUTSIDE_TAG;
    public ArrayList<TagSegment> segmentList;
    private TagSegment currentSegment;
    private final CookieManager cookieManager;

    public TaggedHtml()
    {
        segmentList = null;
        currentSegment = null;
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    public static boolean justSpaces(String in)
    {
        if ( in == null || in.length() < 1 ) {
            return true;
        }
        int i;
        char c;
        for ( i = 0; i < in.length(); i++ ) {
            c = in.charAt(i);
                if ( c != ' ' && c != '\t' && c != '\n' && c != '\r' ) {
            return false;
            }
        }
        return true;
    }

    public static String trimQuotes(String in)
    {
        int l;
        String out;
        out = in;
        l = out.length();
        if ( out.charAt(l-1) == '\"' ) {
            out = out.substring(0, l-1);
        }
        l = out.length();
        if ( out.charAt(0) == '\"' ) {
            out = out.substring(1, l);
        }
        return out;
    }

    public static String trimSpaces(String in)
    {
        int start, end;
        String out;
        out = in;
        char c;

        if ( in == null ) {
            return null;
        }

        if ( in.length() < 1 ) {
            return in;
        }

        if ( justSpaces(in) ) {
            return "";
        }

        for ( start = 0; start < out.length(); start++ ) {
            c = out.charAt(start);
            if ( c != ' ' && c != '\t' && c != '\n' && c != '\r' ) {
                break;
            }
        }

        for ( end = out.length() - 1; end >= 0; end-- ) {
            c = out.charAt(end);
            if ( c != ' ' && c != '\t' && c != '\n' && c != '\r' ) {
                break;
            }
        }

        out = out.substring(start, end + 1);
        return out;
    }

    /**

    @param is
    @return true if this is last page
    */
    private String importDataFromJson(
        InputStream is)
    {
        segmentList = new ArrayList<TagSegment>();

        try {
            //-----------------------------------------------------------------
            byte []  buffer = new byte [4096];

            String msg = "";

            int bytes;
            
            while ( true )  {
                bytes = is.read(buffer);
                if ( bytes <= 0 ) {
                    break;
                }
                msg += new String(buffer, 0, bytes, "UTF8");                
            }
            
            JSONObject jsonObject;
            
            try {
                jsonObject = new JSONObject(msg);                    
            }
            catch ( Exception e ) {
                jsonObject = null;
            }

            if ( jsonObject == null ) {
                return null;
            }
            
            //-----------------------------------------------------------------
            Set<String> keys;
            keys = jsonObject.keySet();

            for ( Object o : keys ) {
                String s = o.toString();
                Object v = jsonObject.get(s);

                if ( s.contains("url") ) {
                    if ( v instanceof String ) {
                        String ss = (String)v;
                        return ss;
                    }
                }
            }
            //-----------------------------------------------------------------
        }
        catch ( IOException | NumberFormatException e ) {
            System.err.println("Error reading processing HTML");
        }
        return null;
    }

    /**
    Creates a new html based web transaction from the specified pageUrl,
    without taking into account any input cookies.
    TODO: Change this to only call another method, one with cookies handling!
    @param pageUrl
    @param cookies
    @param withRedirect true if used in the login phase with redirects
    */
    public void getInternetPage(
        String pageUrl, ArrayList<String> cookies, boolean withRedirect)
    {
        //----------------------------------------------------------------- 
        CloseableHttpClient httpclient;

        HttpGet connection;
        
        try {
            String normalizedUrl;
            normalizedUrl = pageUrl.replace(" ", "%20");
            connection = new HttpGet(pageUrl);
        }
        catch( Exception e ) {
            System.out.println("  * URL inválido, saltando... " + pageUrl);
            return;
        }

        connection.setProtocolVersion(HttpVersion.HTTP_1_1);
        connection.setHeader("Host", getHostFromURL(pageUrl));
        connection.setHeader("Connection", "keep-alive");
        connection.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        connection.setHeader("Upgrade-Insecure-Requests", "1");
        connection.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
        connection.setHeader("DNT", "1");
        connection.setHeader("Referer", "http://www.computrabajo.com.ve/");
        connection.setHeader("Accept-Encoding", "identity" /*"gzip, deflate, sdch"*/);
        connection.setHeader("Accept-Language", "en-US,en;q=0.8,es;q=0.6");
        prepareExistingCookies(cookies, connection);
        //connection.setHeader("Origin", "http://www.computrabajo.com.ve");
        //connection.setHeader("X-Requested-With", "XMLHttpRequest");
        //connection.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        //-----------------------------------------------------------------
        if ( !withRedirect ) {
            //System.out.println("  - Creating HTTP connection without redirection to URL: " + pageUrl);
            httpclient = HttpClients.createDefault();
        }
        else {
            //System.out.println("  - Creating HTTP connection with redirection to URL: " + pageUrl);
            DefaultHttpClient dhttpclient;
            dhttpclient = new DefaultHttpClient();
            MyRedirectStrategy rs;
            rs = new MyRedirectStrategy(connection, cookies);
            dhttpclient.setRedirectStrategy(rs);
            httpclient = dhttpclient;
        }
        
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(connection);

            //System.out.println("  - Response code: " + response.getStatusLine());
        }
        catch ( ClientProtocolException e ) {
            //e.printStackTrace();
            //e.getCause().printStackTrace();
            System.out.println("HTTP redirect error");
        }
        catch ( IOException e ) {
            try {
                Thread.sleep(5000);
            } 
            catch (InterruptedException ex) {
            
            }
            System.out.println("*** Fallo en descarga html, reintento 2");
            try {
                response = httpclient.execute(connection);
            }
            catch ( Exception e2 ) {
                VSDK.reportMessageWithException(this,
                    VSDK.WARNING,
                    "getInternetPage",
                    "HTTP ERROR",
                    e2);                        
            }
        }

        //-----------------------------------------------------------------
        try {
            if ( response != null ) {
                InputStream is;
                is = response.getEntity().getContent();
                importDataFromHtml(is);

                //-----------------------------------------------------------------
                addRecievedCookies(response, cookies);        
            }
        }
        catch ( IOException e ) {
            VSDK.reportMessageWithException(this,
                VSDK.WARNING,
                "getInternetPage",
                "HTTP ERROR",
                e);        
        }

        //-----------------------------------------------------------------
    }

    /**
    Needs to previously been called a get page (using GET method) in order to
    have a cookie set to send.
    @param pageUrl
    @param cookies
    @param login
    @param password
    @return index page
    */
    public TaggedHtml postInternetPageForLogin(
        String pageUrl,
        ArrayList<String> cookies,
        String login,
        String password)
    {
        try {
            HttpPost connection = new HttpPost(pageUrl);

            prepareExistingCookies(cookies, connection);

            connection.setHeader("Host", "www.computrabajo.com.ve");
            connection.setHeader("Connection", "keep-alive");
            connection.setHeader("Accept", "*/*");
            connection.setHeader("Origin", "http://www.computrabajo.com.ve");
            connection.setHeader("X-Requested-With", "XMLHttpRequest");
            connection.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
            connection.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setHeader("DNT", "1");
            connection.setHeader("Referer", "http://www.computrabajo.com.ve/");
            connection.setHeader("Accept-Encoding", "gzip, deflate, sdch");
            connection.setHeader("Accept-Language", "es-ES,es;q=0.8,en;q=0.6");
            connection.setProtocolVersion(HttpVersion.HTTP_1_1);

            //-----------------------------------------------------------------
            // Prepare POST contents request
            String loginString = "pe=" + login + "&pp=" + password + "&rp=0";
            ByteArrayInputStream bais;
            bais = new ByteArrayInputStream(loginString.getBytes());
            byte arr[];
            arr = new byte[bais.available()];
            bais.read(arr);
            ByteArrayEntity reqEntity = new ByteArrayEntity(arr);
            connection.setEntity(reqEntity);
            
            //-----------------------------------------------------------------
            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response;
            response = httpclient.execute(connection);

            addRecievedCookies(response, cookies);

            //-----------------------------------------------------------------
            InputStream is;
            is = response.getEntity().getContent();
            String responseUrl;
            responseUrl = importDataFromJson(is);
            if ( responseUrl != null ) {
                System.out.println("3. Activating authentication tokens");
                TaggedHtml pageProcessor = new TaggedHtml();
                pageProcessor.getInternetPage(responseUrl, cookies, true);
                return pageProcessor;
            }
            response.close();
        }
        catch ( IOException e ) {
            VSDK.reportMessageWithException(this,
                VSDK.WARNING,
                "getInternetPage",
                "HTTP ERROR",
                e);
        }
        return null;
    }

    private void prepareExistingCookies(
        List<String> cookies, 
        HttpRequestBase connection) {
        //-----------------------------------------------------------------
        int i;
        String val = "";
        for ( i = cookies.size() - 1; i >= 0; i-- ) {
            String c;
            c = cookies.get(i);
            if ( c.contains(";") ) {
                int x;
                x = c.indexOf(';');
                c = c.substring(0, x+1);
            }
            val += c + " ";
        }
        val += " _gat=1; _ga=GA1.3.575338493.1446664730";
        if ( cookies.size() > 0 ) {
            connection.setHeader("Cookie", val);
        }
    }

    private void addRecievedCookies(
        CloseableHttpResponse response, ArrayList<String> cookies) 
            throws ParseException {
        //-----------------------------------------------------------------
        // Append new cookies
        Header hs[] = response.getAllHeaders();

        if ( hs != null ) {
            addHeadersToCookies(hs, cookies);
        }
    }

    public static void addHeadersToCookies(
        Header[] hs, ArrayList<String> cookies) throws ParseException {
        //System.out.println("  - Processing headers in HTTP response: " + hs.length);
        int i;
        for ( i = 0; i < hs.length; i++ ) {
            //System.out.println("    . Header: " + hs[i].getName());
            if ( hs[i].getName().equals("Set-Cookie") ) {
                HeaderElement he[] = hs[i].getElements();
                int j;
                for ( j = 0; j < he.length; j++ ) {
                    String cc;
                    cc = he[j].getName() + "=" + he[j].getValue() + ";";
                    if ( !containsCookie(cookies, he[j].getName()) ) {
                        //System.out.println("      -> (*NEW*) " + cc);
                        cookies.add(cc);
                    }
                    else {
                        //System.out.println("      -> " + cc);
                    }
                }
            }
        }
    }

    private void processByteHtml(byte b)
    {
        String content = currentSegment.getContent();
        if ( b == '<' ) {
            if ( currentState == INSIDE_TAG ) {
                //System.err.println("Warning: re-entering tag");
                //System.err.print("[!");
            }
            
            if ( content != null &&
                 content.length() > 0 &&
                 !justSpaces(content) ) {
                //System.out.println("REMANENTE: " + currentSegment.content);
                currentSegment.insideTag = false;
                segmentList.add(currentSegment);
            }

            currentState = INSIDE_TAG;
            currentSegment = new TagSegment();
            currentSegment.insideTag = true;
        }
        currentSegment.append(b);
        if ( b == '>' ) {
            if ( currentState == OUTSIDE_TAG ) {
                //System.err.println("Warning: re-exiting tag");
                //System.err.print("!]");
            }
            segmentList.add(currentSegment);
            currentState = OUTSIDE_TAG;
            currentSegment = new TagSegment();
            currentSegment.insideTag = false;
        }
    }

    /**
    Always erase previous html data, but remember cookies...
    @param is
    */
    public void importDataFromHtml(InputStream is)
    {
        int i;

        currentState = OUTSIDE_TAG;
        if ( segmentList == null ) {
            segmentList = new ArrayList<TagSegment>();
        }
        currentSegment = new TagSegment();
        currentSegment.insideTag = false;
        segmentList.add(currentSegment);

        try {
            //-----------------------------------------------------------------
            byte buffer[] = new byte[4096];
            int bytes;
            while  ( true )  {
                bytes = is.read (buffer);
                if ( bytes <= 0 ) {
                    break;
                }

                for ( i = 0; i < bytes; i++ ) {
                    processByteHtml(buffer[i]);
                }
            }

            //-----------------------------------------------------------------
        }
        catch ( Exception e ) {
            System.err.println("Error reading processing HTML");
        }
    }

    public void exportHtml(OutputStream os)
    {
        TagSegment elem;
        byte []  buffer;
        int i;
        

        for ( i = 0; i < segmentList.size(); i++ ) {
            elem = segmentList.get(i);
            String content = elem.getContent();
            buffer = content.getBytes();
            try {
                os.write(buffer);
            }
            catch ( Exception e ) {
                System.err.println("Error writing to html...");
            }
        }
    }

    public void exportHtml(String filename)
    {
        //-----------------------------------------------------------------
        try {
            File output2 = new File (filename) ;
            FileOutputStream outputStream2;
            outputStream2 = new FileOutputStream(output2);
            exportHtml(outputStream2);
            outputStream2.close();
        }
        catch ( Exception e ) {
            System.err.println("Error exportando tags...");
        }
        //-----------------------------------------------------------------
    }

    public String getUrlFromAHrefContaining(String contentKey)
    {
        int i;
        String tagName;
        String lastUrl = null;

        for ( i = 0; i < segmentList.size(); i++ ) {
            tagName = segmentList.get(i).getTagName();
            if ( tagName != null && tagName.equals("A") ) {
                lastUrl = segmentList.get(i).getTagParameterValue("HREF");
            }
            if ( tagName == null && lastUrl != null ) {
                if ( segmentList.get(i).getContent().contains(contentKey) ) {
                    return trimQuotes(lastUrl);
                }
            }
        }
        return null;
    }

    public HtmlForm getHtmlForm(int index)
    {
        int i;
        String tagName;
        String lastUrl = null;
        HtmlForm form = new HtmlForm();
        int formNumber = -1;
        boolean insideForm = false;

        for ( i = 0; i < segmentList.size(); i++ ) {
            tagName = segmentList.get(i).getTagName();

            if ( tagName != null && tagName.equals("FORM") ) {
                formNumber++;
                insideForm = true;
                ArrayList<TagParameter> tag;
                tag = segmentList.get(i).getTagParameters();
                form.configure(tag);
            }
            if ( tagName != null && tagName.equals("INPUT") &&
		 insideForm && formNumber == index) {
                ArrayList<TagParameter> tag;
                tag = segmentList.get(i).getTagParameters();
                form.addInputFromTag(tag);
            }
            if ( tagName != null && tagName.equals("/FORM") ) {
                insideForm = false;
            }
        }
        return form;
    }

    /**
    Extracts a subpage from `this` TaggedHtml, and returns it
    in the `trimmed` TaggedHtml.  The trimmed page contains
    the `tableIndex` first level table from inside the original
    page.
    @param tableIndex
    @return
    */
    public TaggedHtml extractTrimmedByTable(int tableIndex)
    {
        TaggedHtml trimmed = new TaggedHtml();
        trimmed.segmentList = new ArrayList<TagSegment>();

        int i;
        TagSegment segi;
        String tagName;
        int level = 0;
        int count = -1;
        boolean tableTag = false;

        for ( i = 0; i < segmentList.size(); i++ ) {
            segi = segmentList.get(i);
            tagName = segi.getTagName();
            if ( tagName!= null && tagName.equals("TABLE") ) {
                level++;
                if ( level == 1 ) {
                    count++;
                    tableTag = true;
                }
            }
            if ( tagName!= null && tagName.equals("/TABLE") ) {
                level--;
            }

            if ( level > 0 && count == tableIndex && tableTag == false ) {
                trimmed.segmentList.add(new TagSegment(segi));
            }
            tableTag = false;
        }

        return trimmed;
    }

    public ArrayList<TaggedHtml> extractTableCells()
    {
        ArrayList<TaggedHtml> cells;
        cells = new ArrayList<TaggedHtml>();

        int i;
        TagSegment segi;
        String tagName;
        int level = 0;
        TaggedHtml trimmed = null;

        for ( i = 0; i < segmentList.size(); i++ ) {
            segi = segmentList.get(i);
            tagName = segi.getTagName();
            if ( tagName!= null && tagName.equals("TD") ) {
                level++;
                if ( level == 1 ) {
                    trimmed = new TaggedHtml();
                    trimmed.segmentList = new ArrayList<TagSegment>();
                    cells.add(trimmed);
                }
            }
            if ( tagName!= null && tagName.equals("/TD") ) {
                level--;
            }

            if ( level > 0 && trimmed != null ) {
                trimmed.segmentList.add(new TagSegment(segi));
            }
        }

        return cells;
    }

    private static boolean containsCookie(List<String> cookies, String name) {
        int i;
                
        for ( i = 0; i < cookies.size(); i++ ) {
            if ( cookies.get(i).contains(name) ) {
                return true;
            }
        }
        return false;
    }

    public static String getHostFromURL(String pageUrl) {
        //System.out.println("  * URL: " + pageUrl);
        try {
            String hostname;
            URI u = new URI(pageUrl);
            hostname = u.getHost();
            //System.out.println("  * HOSTNAME: " + hostname);
            return hostname;
        }
        catch ( Exception e ) {
            System.out.println("Error: malformed URL");
            System.exit(9);
        }
        return null;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
