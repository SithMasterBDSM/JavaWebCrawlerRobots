//==============================================================================
package webcrawler;

// Java basic classes
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.HashMap;

// MongoDB classes
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;

// Apache http classes
import org.apache.http.ParseException;

// VSDK classes
import vsdk.toolkit.common.VSDK;

// Application specific classes
import databaseMongo.model.Product;
import java.util.Collection;

/**
*/
public class productProcessor {

    private static void processProductPageForImages(
        IngenioTaggedHtml pageProcessor, Product p) 
    {
        TagSegment ts;
        int i;
        int j;
        String n;
        String v;

        ArrayList<String> imageUrlsToDownload;
        imageUrlsToDownload = new ArrayList<String>();
        for ( i = 0; i < pageProcessor.segmentList.size(); i++ ) {
            ts = pageProcessor.segmentList.get(i);
            for ( j = 0; j < ts.getTagParameters().size(); j++ ) {
                n = ts.getTagParameters().get(j).name;
                v = ts.getTagParameters().get(j).value;
                if ( n.equals("href") ) {
                    v = v.replaceAll("\"", "");
                    if ( v.contains("images/grandes/") && 
                         !v.contains("mailto") ) {
                        v = v.replaceAll(" ", "%20");
                        if ( !imageUrlsToDownload.contains(
                                "http://www.mppromocionales.com/" + v) ) {
                            imageUrlsToDownload.add(
                                "http://www.mppromocionales.com/" + v);
                        }
                    }
                }
            }
        }
        downloadImageList(p, imageUrlsToDownload);
    }

    private static HashMap<String, ProductVariant> processProductPageForVariants(
        IngenioTaggedHtml pageProcessor, Product p) 
    {
        TagSegment ts;
        int i;
        int j;
        String n;
        String v;
        boolean insideVariant = false;
        int divLevel = 0;
        int columnNumber = 0;
        boolean insideData = false;
        ProductVariant pv = new ProductVariant();
        HashMap<String, ProductVariant> variants;
        variants = new HashMap<String, ProductVariant>();
        
        for ( i = 0; i < pageProcessor.segmentList.size(); i++ ) {
            ts = pageProcessor.segmentList.get(i);
            if ( ts.getTagName() == null ) {
                if ( insideData ) {
                    insideData = false;
                    if ( ts.content.contains("/") ) {
                        columnNumber = 0;
                        pv = new ProductVariant();
                    }
                    if ( columnNumber < 0 || columnNumber > 3 ) {
                        continue;
                    }
                    switch ( columnNumber ) {
                      case 0:
                        pv.setCompoundString(ts.content);
                        break;
                      case 1:
                        if ( !ts.content.contains("&nbsp") ) {
                            pv.setQuantityFontibon(Integer.parseInt(ts.content));
                        }
                        break;
                      case 2:
                        if ( !ts.content.contains("&nbsp") ) {
                            pv.setQuantityCelta(Integer.parseInt(ts.content));
                        }
                        break;
                      case 3:
                        if ( !ts.content.contains("&nbsp") ) {
                            pv.setQuantityTotal(Integer.parseInt(ts.content));
                        }
                        break;
                    }
                    if ( pv.isValid() ) {
                        if ( !variants.containsKey(pv.getReference()) ) {
                            //System.out.println("    . " + pv);
                            variants.put(pv.getReference(), pv);
                        }
                    }
                }
                continue;
            }
                        
            String t = ts.getTagName();

            if ( t.equals("DIV") ) {
                if ( insideVariant ) {
                    divLevel++;
                    insideData = true;
                }
                for ( j = 0; j < ts.getTagParameters().size(); j++ ) {
                    n = ts.getTagParameters().get(j).name;
                    v = ts.getTagParameters().get(j).value;
                    if ( n.equals("id") && v.contains("gris") ) {
                        //System.out.println("DIV {");
                        insideVariant = true;
                        divLevel = 1;
                        columnNumber = 1;
                        break;
                    }
                }
            }
            else if ( t.equals("/DIV") && insideVariant ) {
                if ( divLevel == 2 ) {
                    columnNumber++;
                }
                if ( divLevel == 1 ) {
                    //System.out.println("}");
                    insideVariant = false;
                    insideData = false;
                }
                divLevel--;
            }
        }
        return variants;
    }

    private static void downloadImageList(
        Product p, 
        ArrayList<String> imageUrlList) 
    {
        int n;
        n = p.getCode();

        File path;
        path = new File("./output/images/" + n);
        URL url;
        URLConnection urlCon;
        InputStream is;
        FileOutputStream fos;
        int read;
        String urlAux;
        byte array[] = new byte[1000];
        if ( !path.exists() ) {
            path.mkdirs();
        }
        int i;
        for ( i = 0; i < imageUrlList.size(); i++ ) {
            try {
                urlAux = imageUrlList.get(i);
                if ( !urlAux.contains("\\s") ) {
                    url = new URL(urlAux);
                    
                    String filename;
                    filename = path + "/" + n + "_" + 
                        urlPattern(urlAux) + ".jpg";
                    
                    File fd = new File(filename);
                    if ( fd.exists() ) {
                        return;
                    }
                    System.out.println("    . Image URL: " + urlAux);

                    
                    urlCon = url.openConnection();
                    is = urlCon.getInputStream();
                    fos = new FileOutputStream(fd);
                    read = is.read(array);
                    while (read > 0) {
                        fos.write(array, 0, read);
                        read = is.read(array);
                    }
                    is.close();
                    fos.close();
                }
                else {
                    System.out.println("Ignoring image url: [" + urlAux + "]");
                }
            } 
            catch ( IOException e ) {
                VSDK.reportMessageWithException(
                    null, 
                    VSDK.WARNING,
                    "downloadImage", 
                    "Error downloading individual image", 
                    e);
            }
        }
    }
    
    private static void buildProductEntryFromPage(
        DBCollection marPicoProduct,
        IngenioTaggedHtml pageProcessor, String url) 
    {
        if ( pageProcessor.segmentList == null ) {
            System.out.println("Warning: empty page");
        } 
        else {
            Product p;
            p = new Product();
            p.setUrl(url);
            p.setCode(extractProductId(url));

            Date importDate = processProductPageForBasicData(pageProcessor, p);
            System.out.println("  * Nombre: [" + p.getName() + "]");

            if ( p.getName() != null && !p.getName().isEmpty() ) {
                int pid = extractProductId(url);

                if ( !productIsInDatabase(marPicoProduct, pid) ) {
                    insertProductInMongoDatabase(
                        marPicoProduct, p, importDate, pageProcessor, url);
                }
                else {
                    int cid = extractCategoryId(url);
                    addCategoryToProduct(marPicoProduct, pid, cid);
                }
                HashMap<String, ProductVariant> variants;
                variants = processProductPageForVariants(pageProcessor, p);
                updateVariantsInProductDatabase(marPicoProduct, p, variants);
            }
        }
    }

    private static Date processProductPageForBasicData(IngenioTaggedHtml pageProcessor, Product p) {
        TagSegment ts;
        int i;
        boolean doMaterial = false;
        boolean doMeasures = false;
        boolean doPrintArea = false;
        boolean doBrand = false;
        boolean doPacking = false;
        boolean doTitle = false;
        Date importDate = new Date();
        for ( i = 0; i < pageProcessor.segmentList.size(); i++ ) {
            ts = pageProcessor.segmentList.get(i);
            if ( !ts.insideTag ) {
                if ( ts.content.contains("MATERIAL") ) {
                    doMaterial = true;
                }
                else if ( doTitle ) {
                    p.setName(ts.content);
                    doTitle = false;
                }
                else if ( doMaterial ) {
                    String n;
                    String d;
                    p.setMaterial(ts.content);
                    d = pageProcessor.segmentList.get(i - 8).content;
                    p.setDescription(d);
                    doMaterial = false;
                }
                else if ( ts.content.contains("MEDIDAS") ) {
                    doMeasures = true;
                }
                else if ( doMeasures ) {
                    p.setMeasures(ts.content);
                    doMeasures = false;
                }
                else if ( ts.content.contains("REA IMPRESI") ) {
                    doPrintArea = true;
                }
                else if ( doPrintArea ) {
                    p.setPrintArea(ts.content);
                    doPrintArea = false;
                }
                else if ( ts.content.contains("MARCA") ) {
                    doBrand = true;
                }
                else if ( doBrand ) {
                    p.setBrand(ts.content);
                    doBrand = false;
                }
                else if ( ts.content.contains("EMPAQUE") ) {
                    doPacking = true;
                }
                else if ( doPacking ) {
                    p.setPacking(ts.content);
                    doPacking = false;
                }
                if ( ts.content.contains("Precio de") ) {
                    String sub;
                    int index;
                    index = ts.content.indexOf('$');
                    if ( index >= 0 ) {
                        sub = ts.content.substring(index+1);
                        sub = sub.replace(".", "");
                        sub = sub.trim();
                        double price = Double.parseDouble(sub);
                        p.setPrice(price);
                    }
                }
            }
            else {
                String tn = ts.getTagName();
                if ( tn.equals("DIV") ) {
                    int j;
                    for ( j = 0; j < ts.getTagParameters().size(); j++ ) {
                        String n = ts.getTagParameters().get(j).name;
                        String v = ts.getTagParameters().get(j).value;
                        if ( n.equals("style") && v.contains("font-size:26px") ) {
                            doTitle = true;
                        }
                    }
                }
            }
        }
        return importDate;
    }

    private static void insertProductInMongoDatabase(
        DBCollection marPicoProduct,
        Product p, 
        Date importDate, 
        IngenioTaggedHtml pageProcessor, 
        String url) 
    {
        BasicDBObject pdb;
        pdb = new BasicDBObject();
        pdb.append("id", extractProductId(p.getUrl()));
        pdb.append("sourceUrl", p.getUrl());
        pdb.append("name", p.getName());
        pdb.append("description", p.getDescription());
        pdb.append("material", p.getMaterial());
        pdb.append("markingSupported", p.getBrand());
        pdb.append("measures", p.getMeasures());
        pdb.append("printArea", p.getPrintArea());
        pdb.append("price", p.getPrice());
        pdb.append("packing", p.getPacking());
        pdb.append("importDate", importDate);
        
        List<Integer> cidArray;
        cidArray = new ArrayList<Integer>();
        cidArray.add(extractCategoryId(p.getUrl()));
        pdb.append("arrayOfparentCategoriesId", cidArray);
        try {
            marPicoProduct.insert(pdb);
            processProductPageForImages(pageProcessor, p);
        }
        catch ( DuplicateKeyException e ) {
            System.out.println("ERROR: wrong detection schema!");
            System.exit(0);
        }
        catch ( ParseException e ) {
            VSDK.reportMessageWithException(null, VSDK.FATAL_ERROR,
                    "buildProductEntryFromPage", "Parse error", e);
        }
    }

    private static int extractCategoryId(String url) {
        try {
            StringTokenizer parser1;
            parser1 = new StringTokenizer(url, "&");
            while ( parser1.hasMoreTokens() ) {
                String part;
                part = parser1.nextToken();
                if ( part.contains("cat_id") ) {
                    StringTokenizer parser2;
                    parser2 = new StringTokenizer(part, "=");
                    String subpart;
                    parser2.nextToken();
                    subpart = parser2.nextToken();
                    return Integer.parseInt(subpart);
                }
            }
        }
        catch ( Exception e ) {
            
        }
        return 0;
    }

    private static int extractProductId(String url) {
        try {
            StringTokenizer parser1;
            parser1 = new StringTokenizer(url, "&");
            while ( parser1.hasMoreTokens() ) {
                String part;
                part = parser1.nextToken();
                if ( part.contains("idprod") ) {
                    StringTokenizer parser2;
                    parser2 = new StringTokenizer(part, "=");
                    String subpart;
                    parser2.nextToken();
                    subpart = parser2.nextToken();
                    return Integer.parseInt(subpart);
                }
            }
        }
        catch ( Exception e ) {
            
        }
        return 0;
    }

    private static boolean productIsInDatabase(
        DBCollection marPicoProduct, int pid) 
    {
        BasicDBObject searchQuery;
        searchQuery = new BasicDBObject();
        searchQuery.append("id", pid);
        BasicDBObject existingProduct;
        existingProduct = (BasicDBObject)
            marPicoProduct.findOne(searchQuery);
        return existingProduct != null;
    }

    private static void addCategoryToProduct(
        DBCollection marPicoProduct, int pid, int cid) 
    {
        BasicDBObject searchQuery;
        searchQuery = new BasicDBObject();
        searchQuery.append("id", pid);
        BasicDBObject existingProduct;
        existingProduct = (BasicDBObject)
            marPicoProduct.findOne(searchQuery);
        if ( existingProduct == null ) {
            return;
        }
        Object o = existingProduct.get("arrayOfparentCategoriesId");
        if ( o == null ) {
            return;
        }
        
        if ( o instanceof BasicDBList ) {
            BasicDBList arr;
            arr = (BasicDBList)o;
            int i;
            for ( i = 0; i < arr.size(); i++ ) {
                Object e;
                e = arr.get(i);
                if ( e instanceof Integer ) {
                    Integer ii = (Integer)e;
                    if ( ii == cid ) {
                        return;
                    }
                }
            }
            
            arr.add(cid);
                       
            DBObject newValues = new BasicDBObject(
                new BasicDBObject("$set",
                    new BasicDBObject("arrayOfparentCategoriesId", arr)));
            marPicoProduct.update(searchQuery, newValues);
        }
    }

    /**
    Gets specific marPicoProduct from already links on collection "marPicoElementLink".
    Populates collection "productList".
     * @param cookies
    @param marPicoElementLink
    @param marPicoProduct
    */
    public static void downloadProductCategoryPages(
        ArrayList<String> cookies,
        DBCollection marPicoElementLink,
        DBCollection marPicoProduct) 
    {
        BasicDBObject searchKey = new BasicDBObject();
        BasicDBObject options = new BasicDBObject("sourceUrl", 1);
        DBCursor c = marPicoElementLink.find(searchKey, options);

        IngenioTaggedHtml pageProcessor;
        pageProcessor = new IngenioTaggedHtml();
        boolean status = pageProcessor.postInternetPageForLogin(
            "http://www.mppromocionales.com/login.php?url=http://www.mppromocionales.com/index.php",
            cookies,
            "ejecorp",
            "ejecorp"
        );
        
        if ( !status ) {
            System.out.println("ERROR LOGIN");
            System.exit(1);
        }
        
        int i;
        for ( i = 0; i < cookies.size(); i++ ) {
            System.out.println("  - " + cookies.get(i));
        }
        
        while ( c.hasNext() ) {
            DBObject ei;
            ei = c.next();

            String url;
            url = ei.get("sourceUrl").toString();
            
            if ( url.contains("detallesvar.php") ) {
                System.out.println("  - Adding product: " + url);
                pageProcessor = new IngenioTaggedHtml();
                pageProcessor.getInternetPage(url, cookies, false);
                buildProductEntryFromPage(marPicoProduct, pageProcessor, url);
            }
        }
    }

    private static String urlPattern(String url) 
    {
        int i;
        int n = url.length();
        for ( i = n-1; i >= 0; i-- ) {
            char c = url.charAt(i);
            if ( c == '/' ) {
                break;
            }
        }
        String sub = url.substring(i);
        StringTokenizer parser = new StringTokenizer(sub, "/.?&");
        return parser.nextToken();
    }

    private static void updateVariantsInProductDatabase(
        DBCollection marPicoProduct,
        Product p, HashMap<String, ProductVariant> variants) 
    {
        int n = variants.size();
        
        if ( n < 1 ) {
            System.out.println("ERROR: THERE IS NO VARIANTS!");
            System.exit(1);
        }
        
        //System.out.println("    . Number of variants: " + n);
        BasicDBObject searchKey;
        searchKey = new BasicDBObject("id", p.getCode());
        DBObject dbObject;
        dbObject = marPicoProduct.findOne(searchKey);
        if ( dbObject == null ) {
            return;
        }
        
        ArrayList<DBObject> arr;
        arr = new ArrayList<DBObject>();
        
        Collection<ProductVariant> s = variants.values();
        
        for ( ProductVariant pv : s ) {
            BasicDBObject dbo = new BasicDBObject();
            dbo.append("reference", pv.getReference());
            dbo.append("description", pv.getDescription());
            dbo.append("quantityFontibon", pv.getQuantityFontibon());
            dbo.append("quantityCelta", pv.getQuantityCelta());
            dbo.append("quantityTotal", pv.getQuantityTotal());
            arr.add(dbo);
        }
        
        DBObject newValues = new BasicDBObject(
            new BasicDBObject("$set",
                new BasicDBObject("arrayOfVariants", arr)));
        
        marPicoProduct.update(searchKey, newValues);
    }
}

//==============================================================================
//= EOF                                                                        =
//==============================================================================
