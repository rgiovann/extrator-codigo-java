package github.exporter;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.databind.*;

/**
 * Uma classe utilitária para buscar e consolidar arquivos fonte Java de um repositório GitHub.
 * Esta classe utiliza a API do GitHub para obter a árvore de arquivos do repositório e baixar
 * arquivos Java localizados em diretórios de código-fonte (ex.: contendo "src/"), excluindo
 * diretórios de teste. O conteúdo dos arquivos Java é salvo em um único arquivo de texto com
 * um nome que inclui a data e hora.
 *
 * @author Giovanni Leopoldo Rozza
 * @version 1.0
 * @since 03.05.2025
 */
public class GitHubJavaFileFetcher {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/";

    public static void main(String[] args) throws IOException, InterruptedException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Digite o repositório (ex: usuario/repositorio): ");
            String repo = scanner.nextLine().trim();

            String branch = "main"; // ou "master"
            String apiUrl = GITHUB_API_URL + repo + "/git/trees/" + branch + "?recursive=1";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFile = repo.replace("/", "_") + "_" + timestamp + ".txt";

            List<String> javaFiles = new ArrayList<>();

            ObjectMapper mapper = new ObjectMapper();
            HttpClient httpClient = HttpClient.newHttpClient();

            // --- Requisição HTTP para obter a árvore do repositório
            HttpRequest apiRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            HttpResponse<InputStream> apiResponse = httpClient.send(apiRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (apiResponse.statusCode() != 200) {
                System.err.println("Erro ao acessar a API do GitHub: " + apiResponse.statusCode());
                return;
            }

            JsonNode treeJson = mapper.readTree(apiResponse.body());

            for (JsonNode file : treeJson.get("tree")) {
                String path = file.get("path").asText();
                if (path.endsWith(".java") && path.contains("src/") && !path.contains("test/")) {
                    javaFiles.add(path);
                }
            }

            Path outputPath = Paths.get(outputFile);
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                for (String path : javaFiles) {
                    String rawUrl = "https://raw.githubusercontent.com/" + repo + "/" + branch + "/" + path;
                    writer.write("-- inicio arquivo: " + path);
                    writer.newLine();

                    HttpRequest fileRequest = HttpRequest.newBuilder()
                            .uri(URI.create(rawUrl))
                            .header("Accept", "application/vnd.github.v3.raw")
                            .build();

                    HttpResponse<String> fileResponse = httpClient.send(fileRequest, HttpResponse.BodyHandlers.ofString());

                    if (fileResponse.statusCode() == 200) {
                        writer.write(fileResponse.body());
                        writer.newLine();
                    } else {
                        System.err.println("Erro ao acessar: " + rawUrl + " (status: " + fileResponse.statusCode() + ")");
                    }

                    writer.write("-- fim arquivo");
                    writer.newLine();
                    writer.newLine();
                }
            }

            System.out.println("Arquivo gerado com sucesso: " + outputFile);
        }
    }
}
