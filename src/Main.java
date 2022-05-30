package com.carrieforle.uploadtoimgur;

import java.net.http.*;
import java.net.URI;
import java.net.URL;
import java.awt.Desktop;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.RenderedImage;
import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

public class Main
{
    public final static String CLIENT_ID = "f88334fd9f04f92";
    
    enum PATH {obj}
    enum SRC_HTML {obj}
    enum BINARY {obj}
    
    public static void main(String[] args)
    {
        try
        {
            if(args.length == 0)
            {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                byte[] data = null;
                
                if(clipboard.isDataFlavorAvailable(new DataFlavor("text/html")))
                {
                    data = getFromClipboard(clipboard, SRC_HTML.obj);
                }
                
                else if(clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor))
                {
                    data = getFromClipboard(clipboard, BINARY.obj);
                }
                
                else if(clipboard.isDataFlavorAvailable(new DataFlavor("application/x-java-file-list; class=java.util.List")))
                {
                    args = getFromClipboard(clipboard, PATH.obj);
                }
                
                else
                {
                    return;
                }
                                
                // If using data(single file) rather than path(single or multile files)
                if(data != null)
                {
                    HttpResponse<String> httpR_response = uploadToImgur(data);
                    
                    // Uploading too fast
                    if(httpR_response.statusCode() == 429)
                    {
                        System.out.println("You're uploading too fast. Please have a coffee and wait patiently for 2 minutes.");
                        System.in.read();
                        return;
                    }
                    
                    else if(httpR_response.statusCode() != 200)
                    {
                        return;
                    }
                    
                        
                    String response = httpR_response.body();
                    String link = extractFrom(response, "link")[0];
                    String[] additional_links = null;
                    
                    if(link.substring(link.length()-3).equals("gif"))
                    {
                        additional_links = extractFrom(response, "mp4", "gifv", "hls");
                        System.out.format("Link: %s\n\n", link);
                        for(String addi_link : additional_links)
                        {
                            System.out.println(addi_link);
                        }
                        System.out.print("\n\nPress Enter to exit");
                        System.in.read();
                    }
                    
                    else
                    {
                        goToWebsite(link);
                        return;
                    }
                }
            }
            
            for(String path : args)
            {
                HttpResponse<String> httpR_response = uploadToImgur(path);
                if(httpR_response.statusCode() == 429)
                {
                    System.out.println("You're uploading too fast. Please have a coffee and wait patiently for 2 minutes.");
                    System.in.read();
                    return;
                }
                
                else if(httpR_response.statusCode() != 200)
                {
                    if(args.length == 1) return;
                    
                    System.out.format("%s > FAILED\n", path.substring(path.lastIndexOf('\\')+1));
                    continue;
                }
                                
                String response = httpR_response.body();
                String link = extractFrom(response, "link")[0];
                String[] additional_links = null;
                
                if(args.length > 1)
                {
                    System.out.format("%s > %s\n", path.substring(path.lastIndexOf('\\')+1), link);
                }
                
                if(link.substring(link.length()-3).equals("gif"))
                {
                    if(args.length == 1)
                    {
                        System.out.format("%s > %s\n", path.substring(path.lastIndexOf('\\')+1), link);
                    }
                    additional_links = extractFrom(response, "mp4", "gifv", "hls");
                    
                    System.out.println();
                    for(String addi_link : additional_links)
                    {
                        System.out.println(addi_link);
                    }
                    System.out.println();
                    
                    if(args.length == 1)
                    {
                        System.out.println("Press Enter to exit");
                        System.in.read();
                    }
                }
                
                else if(args.length == 1)
                {
                    goToWebsite(link);
                    return;
                }
                
            }
            if(args.length > 1)
            {
                System.out.println("Press Enter to exit");
                System.in.read();
            }
        }
        catch(Exception e)
        {
            System.out.println(e);
            System.out.print("Failed");
            try
            {
                System.in.read();
            }
            catch(Exception a)
            {}
        }
    }
    
    public static byte[] getFromClipboard(Clipboard clipboard, SRC_HTML s) throws IOException, ClassNotFoundException, UnsupportedFlavorException
    {
        var flavor = new DataFlavor("text/html");

        try(InputStream is_from_clipboard = (InputStream)clipboard.getData(flavor))
        {
            String str_url = new String(is_from_clipboard.readAllBytes());
            str_url = str_url.substring(10, str_url.length()-3);
            URL url = new URL(str_url);
            
            try(InputStream is_from_url = url.openStream())
            {
                return is_from_url.readAllBytes();
            }
        }
    }
    
    public static byte[] getFromClipboard(Clipboard clipboard, BINARY b) throws IOException, ClassNotFoundException, UnsupportedFlavorException
    {
        var flavor = DataFlavor.imageFlavor;
        RenderedImage data = (RenderedImage)clipboard.getData(flavor);
        try(var out = new ByteArrayOutputStream())
        {
            ImageIO.write(data, "png", out);
            
            return out.toByteArray();
        }
    }
    
    public static String[] getFromClipboard(Clipboard clipboard, PATH p) throws IOException, ClassNotFoundException, UnsupportedFlavorException
    {
        var flavor = new DataFlavor("application/x-java-file-list; class=java.util.List");
        List<?> datas = (List<?>)clipboard.getData(flavor);
        String[] res = new String[datas.size()];
        for(int i = 0; i < res.length; i++)
        {
            res[i] = ((File)datas.get(i)).toString();
        }
        return res;
    }
    
    public static void goToWebsite(String url) throws IOException
    {
        if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        {
            URI u = URI.create(url);  
            Desktop.getDesktop().browse(u);
        }
    }
    
    public static String[] extractFrom(String str, String... words)
    {
        int from_pos = 0;
        String[] res = new String[words.length];
        
        for(int i = 0; i < words.length; i++)
        {
            int start = str.indexOf(words[i], from_pos) + words[i].length()+3;
            
            int end = str.indexOf('\"', start);
            from_pos = end+2;
            StringBuilder sb = new StringBuilder(str.substring(start, end));
            
            for(int j = 0; j < sb.length(); j++)
            {
                if(sb.charAt(j) == '\\')
                {
                    sb.deleteCharAt(j);
                    j--;
                }
            }
            
            res[i] = sb.toString();
        }
        return res;
    }
    
    public static HttpResponse<String> uploadToImgur(byte[] data) throws IOException, InterruptedException
    {
        String uuid = java.util.UUID.randomUUID().toString();
        HttpRequest.BodyPublisher body = oneFileBody(data, uuid);
        URI u = URI.create("https://api.imgur.com/3/upload/");
        
        HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();
        
        HttpRequest request = HttpRequest.newBuilder()
        .uri(u)
        .header("Content-Type", "multipart/form-data; boundary=" + uuid)
        .header("Authorization", "Client-ID " + CLIENT_ID)
        .POST(body)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }
    
    public static HttpResponse<String> uploadToImgur(String path) throws IOException, InterruptedException
    {
        String uuid = java.util.UUID.randomUUID().toString();
        HttpRequest.BodyPublisher body = oneFileBody(Files.readAllBytes(Paths.get(path)), uuid);
        URI u = URI.create("https://api.imgur.com/3/upload/");
        
        HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();
        
        HttpRequest request = HttpRequest.newBuilder()
        .uri(u)
        .header("Content-Type", "multipart/form-data; boundary=" + uuid)
        .header("Authorization", "Client-ID " + CLIENT_ID)
        .POST(body)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }
    
    public static HttpRequest.BodyPublisher oneFileBody(byte[] data, String uuid) throws IOException
    {
        String base64_image = Base64.getEncoder().encodeToString(data);
        String content = String.format("--%1$s\nContent-Disposition: form-data; name=\"type\"\nContent-Type: text/plain\n\nbase64\n--%1$s\nContent-Disposition: form-data; name=\"image\"\n\n%2$s\n--%1$s", uuid, base64_image);

        var body = HttpRequest.BodyPublishers.ofString(content);
        return body;
    }
}