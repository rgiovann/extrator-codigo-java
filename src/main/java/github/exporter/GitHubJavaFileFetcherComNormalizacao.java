package github.exporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

/**
 * Uma classe utilitária para buscar, normalizar e consolidar arquivos fonte Java de um repositório GitHub.
 * Utiliza a API do GitHub para obter a árvore de arquivos do repositório, baixa arquivos Java localizados
 * em diretórios de código-fonte (ex.: contendo "src/"), exclui diretórios de teste e normaliza o conteúdo
 * removendo comentários, extraindo declarações (classe, interface, enum, etc.) e identificando pacotes.
 * O resultado é salvo em um arquivo de texto com nome baseado no repositório e na data/hora.
 *
 * @author Giovanni L. Rozza
 * @version 1.0
 * @since 03.04.2025
 */
public class GitHubJavaFileFetcherComNormalizacao {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/";

    public static void main(String[] args) throws IOException, InterruptedException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Digite o repositório (ex: usuario/repositorio): ");
            String repo = scanner.nextLine().trim();

            String branch = "main";
            String apiUrl = GITHUB_API_URL + repo + "/git/trees/" + branch + "?recursive=1";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFile = repo.replace("/", "_") + "_" + timestamp + ".txt";

            List<String> javaFiles = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            HttpClient httpClient = HttpClient.newHttpClient();

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
                    HttpRequest fileRequest = HttpRequest.newBuilder()
                            .uri(URI.create(rawUrl))
                            .header("Accept", "application/vnd.github.v3.raw")
                            .build();

                    HttpResponse<String> fileResponse = httpClient.send(fileRequest, HttpResponse.BodyHandlers.ofString());

                    if (fileResponse.statusCode() == 200) {
                        String rawCode = fileResponse.body();
                        String cleanedCode = removeComments(rawCode);
                        String declaration = extractDeclaration(cleanedCode);
                        String packageName = extractPackageName(path);

                        writer.write("// FILE: " + path);
                        writer.newLine();
                        writer.write("// PACKAGE: " + packageName);
                        writer.newLine();
                        writer.write("// DECLARATION: " + declaration);
                        writer.newLine();
                        writer.newLine();

                        writer.write(cleanedCode);
                        writer.newLine();

                        writer.write("// END_OF_FILE");
                        writer.newLine();
                        writer.newLine();
                    } else {
                        System.err.println("Erro ao acessar: " + rawUrl + " (status: " + fileResponse.statusCode() + ")");
                    }
                }
            }

            System.out.println("Arquivo gerado com sucesso: " + outputFile);
        }
    }

    private static String removeComments(String code) {
        String semBlocos = code.replaceAll("(?s)/\\*.*?\\*/", "");
        String[] linhas = semBlocos.split("\\R");
        StringBuilder resultado = new StringBuilder();

        for (String linha : linhas) {
            String semLinhaComentario = linha.replaceAll("//.*", "").stripTrailing();
            if (!semLinhaComentario.isBlank()) {
                resultado.append(semLinhaComentario).append("\n");
            }
        }

        return resultado.toString();
    }

    private static String extractDeclaration(String code) {
        Matcher matcher = Pattern.compile("\\b(class|interface|enum|record|@interface)\\s+(\\w+)").matcher(code);
        return matcher.find() ? matcher.group(1) + " " + matcher.group(2) : "N/A";
    }

    private static String extractPackageName(String path) {
        String subpath = path.replaceFirst(".*src/(main|java|src)/java/", "");
        int lastSlash = subpath.lastIndexOf('/');
        return lastSlash > 0
                ? subpath.substring(0, lastSlash).replace('/', '.')
                : "(default)";
    }
}
