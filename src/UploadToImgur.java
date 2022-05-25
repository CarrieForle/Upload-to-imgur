import java.net.http.*;
import java.net.URI;
import java.awt.Desktop;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.IOException;

public class UploadToImgur
{
    public final static String CLIENT_ID = "f88334fd9f04f92";
    
    public static void main(String[] args)
    {
        try
        {
            if(args.length == 0)
                return;
            
            for(String path : args)
            {
                HttpResponse<String> httpR_response = upload_to_imgur(path);
                if(httpR_response.statusCode() != 200)
                {
                    System.out.format("%s FAILED\n", path.substring(path.lastIndexOf('/')+1));
                    continue;
                }
                
                String response = httpR_response.body();
                StringBuilder sb = new StringBuilder(response.substring(response.lastIndexOf("link")+7, response.lastIndexOf("\"},\"")));
                
                for(int i = 0; i < sb.length(); i++)
                {
                    if(sb.charAt(i) == '\\')
                    {
                        sb.deleteCharAt(i);
                        i--;
                    }
                }
                
                String link = sb.toString();
                
                if(args.length == 1)
                {
                    go_to_website(link);
                    return;
                }
                
                else
                {
                    System.out.format("%s > %s\n", path.substring(path.lastIndexOf('/')+1), link);
                }
            }
            
            System.out.println("\nPress Enter to exit");
            Scanner sc = new Scanner(System.in);
            sc.nextLine();
        }
        catch(Exception e)
        {
            System.out.println(e);
            System.out.println("Exception thrown");
        }
    }
    
    public static void go_to_website(String url) throws IOException
    {
        if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        {
            URI u = URI.create(url);  
            Desktop.getDesktop().browse(u);
        }
    }
    
    public static HttpResponse<String> upload_to_imgur(String path) throws IOException, InterruptedException
    {
        var encoder = Base64.getEncoder();
        String uuid = java.util.UUID.randomUUID().toString();
        HttpRequest.BodyPublisher body = one_file_body(path, uuid);
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
    
    public static HttpRequest.BodyPublisher one_file_body(String path, String uuid) throws IOException
    {
        byte[] b_image = Files.readAllBytes(Paths.get(path));
        String base64_image = Base64.getEncoder().encodeToString(b_image);
        
        var body = HttpRequest.BodyPublishers.ofString(String.format("--%1$s\nContent-Disposition: form-data; name=\"type\"\nContent-Type: form-data\n\nbase64\n--%1$s\nContent-Disposition: form-data; name=\"image\"\n\n%2$s\n%1$s", uuid, base64_image));
        return body;
    }
}