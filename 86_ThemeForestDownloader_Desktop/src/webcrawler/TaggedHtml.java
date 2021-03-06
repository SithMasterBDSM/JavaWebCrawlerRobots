//===========================================================================

package webcrawler;

// Java classes
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URL;
import java.net.HttpURLConnection;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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
    private final CookieManager cookies;

    private static String jsonString;
    private static boolean insideBracket;
    private static boolean insideString;
    private final HashMap<String, String> jsonVariables;

    private String jsonStage1String;
    private String jsonStage2String;

    public TaggedHtml()
    {
        segmentList = null;
        currentSegment = null;
        cookies = new CookieManager();
        CookieHandler.setDefault(cookies);
        jsonVariables = new HashMap<String, String>();
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
    private boolean importDataFromJson(
        InputStream is, AtomicInteger pageNumber)
    {
        int i;
        boolean isLast = true;

        currentState = OUTSIDE_TAG;
        segmentList = new ArrayList<TagSegment>();
        currentSegment = new TagSegment();
        currentSegment.insideTag = false;
        segmentList.add(currentSegment);

        try {
            //-----------------------------------------------------------------
            byte []  buffer = new byte [4096];

            String msg = "";

            int bytes;
            while  ( true )  {
                bytes = is.read ( buffer ) ;
                if ( bytes <= 0 )  break;

                for ( i = 0; i < bytes; i++ ) {
                    processByteJson(buffer[i]);
                }

                msg += new String(buffer, "UTF8");
            }

            //-----------------------------------------------------------------
            Set<String> keys;
            keys = jsonVariables.keySet();

            for ( Object o : keys ) {
                String s = o.toString();
                String v = jsonVariables.get(s);

                //System.out.println("JSON: " + s);
                //System.out.println("  - VAL: " + v);

                if ( s.contains("inmueblesHTML")) {
                    InputStream iss = new ByteArrayInputStream(jsonVariables.get(s).getBytes());

                    importDataFromHtml(iss);
                }
                else if ( s.contains("esUltimaPagina") ) {
                    isLast = !v.equals("false");
                }
                else if ( s.contains("numeroPaginaActual") ) {
                    pageNumber.set(Integer.parseInt(trimQuotes(v)));
                }
            }
            //-----------------------------------------------------------------
        }
        catch ( IOException | NumberFormatException e ) {
            System.err.println("Error reading processing HTML");
        }

        return isLast;
    }

    /**
    Creates a new html based web transaction from the specified pageUrl,
    without taking into account any input cookies.
    TODO: Change this to only call another method, one with cookies handling!
    @param pageUrl
    */
    public void getInternetPage(String pageUrl)
    {
        //-----------------------------------------------------------------
        HttpURLConnection connection;
        URL server;
        int code;

        try {
            server = new URL(pageUrl);

            connection = (HttpURLConnection)server.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            code = connection.getResponseCode();

            //-----------------------------------------------------------------
            int hi = 1;

            String headerline;

            while ( true ) {
                headerline = connection.getHeaderField(hi);
                if ( headerline != null ) {
                    //System.out.println(headerline);
                }
                else {
                    break;
                }
                hi++;
            }

            //-----------------------------------------------------------------
            InputStream is;
            is = connection.getInputStream();

            importDataFromHtml(is);
            connection.disconnect();
        }
        catch ( Exception e ) {
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
    @param pageNumber
    @param jsonFilename
    @return
    */
    public boolean postInternetPage(
        String pageUrl,
        AtomicInteger pageNumber,
        String jsonFilename)
    {
        boolean isLast = true;
        try {
            HttpPost connection = new HttpPost(pageUrl);

            ArrayList<HttpCookie> c;
            c = new ArrayList<HttpCookie>();

            int i;
            String val = "";

            for ( i = c.size() - 1; i >= 0; i-- ) {
                val += c.get(i).getName() + "=" + c.get(i).getValue();
                if ( i > 0  ) {
                    val += "; ";
                }
            }

            val +=
                "midciudad=1; "
                + "midtipoinmueble=1; "
                + "midtiponegocio=2; "
                + "midtipocategoriainmueble=1; "
                + "s_nr=1431417993567-Repeat; "
                + "s_lv=1431417993567; "
                + "s_vnum=1432951318923%26vn%3D3";
                /*
                + "s_v10=%5B%5B%27Typed%2FBookmarked%27%2C%271430359318925%27%5D%2C%5B%27Typed%2FBookmarked%27%2C%271430359322907%27%5D%2C%5B%27Typed%2FBookmarked%27%2C%271430359324957%27%5D%2C%5B%27Typed%2FBookmarked%27%2C%271430359328331%27%5D%2C%5B%27Typed%2FBookmarked%27%2C%271430359466061%27%5D%5D; "
                + "s_v8=%5B%5B%27natural%2520search%253A%2520google%253A%2520keyword%2520unavailable%27%2C%271430359318926%27%5D%2C%5B%27natural%2520search%253A%2520google%253A%2520keyword%2520unavailable%27%2C%271430359322908%27%5D%2C%5B%27natural%2520search%253A%2520google%253A%2520keyword%2520unavailable%27%2C%271430359324957%27%5D%2C%5B%27natural%2520search%253A%2520google%253A%2520keyword%2520unavailable%27%2C%271430359328331%27%5D%2C%5B%27natural%2520search%253A%2520google%253A%2520keyword%2520unavailable%27%2C%271430359466061%27%5D%5D; "
                + "_ga=GA1.2.657884234.1430359467; "
                + "_conv_v=vi:1430359539961-0.7660904447881989|sc:3|cs:1431417993|fs:1430359540|pv:18|exp:{10017621.{v.100125776-g.{100111371.1-100111372.1}}}|ps:1431404670; "
                + "_conv_r=s:www.metrocuadrado.com|m:referral|t:; "
                + "s_cc=true; "
                + "s_sq=%5B%5BB%5D%5D; "
                + "idDetallesRelacionados=2699-771113,3181-775522,713-907078; "
                + "_dc_gtm_UA-42368290-2=1; "
                + "gpv_pn=metrocuadrado%3A%20buscar%3A%20resultados%20inmuebles; "
                + "s_invisit=true; "
                + "s_lv_s=Less%20than%201%20day; "
                + "_conv_s=si:3|pv:1";
                */
            connection.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:37.0) Gecko/20100101 Firefox/37.0");
            connection.setHeader("Cookie", val);
            connection.setHeader("X-Requested-With", "XMLHttpRequest");
            connection.setHeader("Pragma", "no-cache");
            connection.setHeader("Connection", "keep-alive");
            connection.setHeader("Cache-Control", "no-cache");
            connection.setHeader("Content-Type", "application/json; charset=UTF-8");
            connection.setHeader("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3");
            connection.setHeader("Accept", "*/*");
            connection.setHeader("Referer", "http://www.metrocuadrado.com/web/buscarFiltros/bogota-apartamento-arriendo");
            connection.setProtocolVersion(HttpVersion.HTTP_1_1);

            File fd = new File(jsonFilename);
            FileInputStream fis = new FileInputStream(fd);

            byte arr[];
            arr = new byte[fis.available()];

            fis.read(arr);

            ByteArrayEntity reqEntity = new ByteArrayEntity(arr);

            connection.setEntity(reqEntity);

            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response;
            response = httpclient.execute(connection);

            //-----------------------------------------------------------------
            InputStream is;
            is = response.getEntity().getContent();

            isLast = importDataFromJson(is, pageNumber);

            response.close();
        }
        catch ( IOException | IllegalStateException e ) {
            VSDK.reportMessageWithException(this,
                VSDK.WARNING,
                "getInternetPage",
                "HTTP ERROR",
                e);
        }
        return isLast;
    }

    private void processByteHtml(byte b)
    {
        if ( b == '<' ) {
            if ( currentState == INSIDE_TAG ) {
                //System.err.println("Warning: re-entering tag");
                //System.err.print("[!");
            }

            if ( currentSegment.content != null &&
                 currentSegment.content.length() > 0 &&
                 !justSpaces(currentSegment.content) ) {
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

    private void processByteJson(byte b)
    {
        char c;

        c = (char)b;

        if ( c == '\"' ) {
            insideString = !insideString;
        }

        if ( c == '[' && !insideString ) {
            insideBracket = true;
        }
        else if ( c == ']' && !insideString ) {
            insideBracket = false;
        }
        if ( c == '}' ) {
            currentState = OUTSIDE_TAG;
            insideBracket = false;
        }
        else if ( c == '{' ) {
            currentState = INSIDE_TAG;
            jsonString = "";
        }
        else if ( currentState == INSIDE_TAG ) {
            boolean add = true;
            if ( c == ':' && !insideBracket && !insideString ) {
                jsonStage1String = jsonString;
                jsonString = "";
                add = false;
            }
            else if ( c == ',' && !insideBracket ) {
                jsonStage2String = jsonString;
                jsonString = "";
                add = false;
                jsonVariables.put(jsonStage1String, jsonStage2String);
            }

            if ( add ) {
                jsonString += c;
            }
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
        segmentList = new ArrayList<TagSegment>();
        currentSegment = new TagSegment();
        currentSegment.insideTag = false;
        segmentList.add(currentSegment);

        try {
            //-----------------------------------------------------------------
            byte []  buffer = new byte [4096];

            int bytes;
            while  ( true )  {
                bytes = is.read ( buffer ) ;
                if ( bytes <= 0 )  break;

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
            buffer = elem.content.getBytes();
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
                if ( segmentList.get(i).content.contains(contentKey) ) {
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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
