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
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class Main
{
    public final static String CLIENT_ID = "f88334fd9f04f92";
    
    public static void main(String[] args)
    {
        
        boolean openBrowser = true;
        final boolean systemConsoleAvailable = (System.console() != null);
        
        try
        {
            List<Integer> paths_index = new ArrayList<>();
            
            // Access arguments and filter out the picture datas
            for(int i = 0 ; i < args.length; i++)
            {
                if(args[i].equals("-f"))
                {
                    openBrowser = false;
                }
                
                else
                {
                    paths_index.add(i);
                }
            }
            final int ps = paths_index.size();
            
            if(ps == 0)
            {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                byte[] data = null;
                
                if(clipboard.isDataFlavorAvailable(new DataFlavor("text/html")))
                {
                    data = getFromClipboardHTML(clipboard);
                }
                
                else if(clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor))
                {
                    data = getFromClipboardBINARY(clipboard);
                }
                
                else if(clipboard.isDataFlavorAvailable(new DataFlavor("application/x-java-file-list; class=java.util.List")))
                {
                    args = getFromClipboardPATH(clipboard);
                    for(int i = 0; i < args.length; i++)
                    {
                        paths_index.add(i);
                    }
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
                        if(systemConsoleAvailable)
                        {
                            System.out.println("You're uploading too fast. Please have a coffee and wait patiently for 2 minutes.");
                            System.in.read();
                        }
                        return;
                    }
                    
                    else if(httpR_response.statusCode() != 200)
                    {
                        return;
                    }
                        
                    String response = httpR_response.body();
                    String link = extractFrom(response, "link")[0];
                    String[] additional_links = null;
                    
                    if(systemConsoleAvailable && link.substring(link.length()-3).equals("gif"))
                    {
                        additional_links = extractFrom(response, "mp4", "gifv", "hls");
                        
                        System.out.format("Link: %s\n\n", link);
                        for(String addi_link : additional_links)
                        {
                            System.out.println(addi_link);
                        }
                    }
                    
                    else if(openBrowser)
                    {
                        goToWebsite(link);
                        return;
                    }
                    
                    else if(systemConsoleAvailable)
                    {
                        System.out.println(link);
                    }
                }
            }
            
            for(int i : paths_index)
            {
                HttpResponse<String> httpR_response = uploadToImgur(args[i]);
                if(httpR_response.statusCode() == 429)
                {
                    if(systemConsoleAvailable)
                    {
                        System.out.println("You're uploading too fast. Please have a coffee and wait patiently for 2 minutes.");
                        System.in.read();
                    }
                    return;
                }
                
                else if(httpR_response.statusCode() != 200)
                {
                    if(ps == 1) return;
                    
                    System.out.format("%s > FAILED\n", args[i].substring(args[i].lastIndexOf('\\')+1));
                    continue;
                }
                                
                String response = httpR_response.body();
                String link = extractFrom(response, "link")[0];
                String[] additional_links = null;
                boolean is_gif = link.substring(link.length()-3).equals("gif");
                
                if(systemConsoleAvailable && (ps > 1 || is_gif))
                {
                    System.out.format("%s > %s\n", args[i].substring(args[i].lastIndexOf('\\')+1), link);
                }
                
                else if(openBrowser && (ps == 1 || !systemConsoleAvailable))
                {
                    goToWebsite(link);
                    return;
                }
                
                else if(systemConsoleAvailable)
                {
                    System.out.println(link);
                }
                
                if(systemConsoleAvailable && is_gif)
                {
                    additional_links = extractFrom(response, "mp4", "gifv", "hls");
                    
                    System.out.println();
                    for(String addi_link : additional_links)
                    {
                        System.out.println(addi_link);
                    }
                    System.out.println();
                }
            }
            
            if(systemConsoleAvailable && (!openBrowser || ps > 1))
            {
                System.out.print("\nPress Enter to exit");
                System.in.read();
            }
        }
        
        catch(Exception e)
        {
            if(!systemConsoleAvailable)
            {
                return;
            }
            
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
    
    public static byte[] getFromClipboardHTML(Clipboard clipboard) throws IOException, ClassNotFoundException, UnsupportedFlavorException
    {
        var flavor = new DataFlavor("text/html");
        try(InputStream is_from_clipboard = (InputStream)clipboard.getData(flavor))
        {
            String str_url = new String(is_from_clipboard.readAllBytes());

            int i_start = 0;
            
            // If copied binary image data from a HTML
            if(str_url.startsWith("<img "))
            {
                i_start = str_url.indexOf("src=\"") + 5;
            }
            
            // Else if copied URL from a HTML
            else if(str_url.startsWith("<a "))
            {
                i_start = str_url.indexOf("href=\"") + 6;
            }
            
            else
            {
                throw new IOException("Image unsupported or corrupted.");
            }
            
            str_url = str_url.substring(i_start, str_url.indexOf('\"', i_start));
            URL url = new URL(str_url);
            
            try(InputStream is_from_url = url.openStream())
            {
                return is_from_url.readAllBytes();
            }
        }
    }
    
    public static byte[] getFromClipboardBINARY(Clipboard clipboard) throws IOException, ClassNotFoundException, UnsupportedFlavorException
    {
        var flavor = DataFlavor.imageFlavor;
        RenderedImage data = (RenderedImage)clipboard.getData(flavor);
        try(var out = new ByteArrayOutputStream())
        {
            ImageIO.write(data, "png", out);
            
            return out.toByteArray();
        }
    }
    
    public static String[] getFromClipboardPATH(Clipboard clipboard) throws IOException, ClassNotFoundException, UnsupportedFlavorException
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
            int start = str.indexOf(words[i], from_pos) + words[i].length() + 3;
            
            int end = str.indexOf('\"', start);
            from_pos = end + 2;
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