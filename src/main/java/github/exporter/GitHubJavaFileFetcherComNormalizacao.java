package github.exporter;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GitHubJavaFileFetcherComNormalizacao {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    private static String repo = "";

    public GitHubJavaFileFetcherComNormalizacao() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Digite o repositório (ex: usuario/repositorio): ");
            repo = scanner.nextLine().trim();
            new GitHubJavaFileFetcherComNormalizacao().extractJavaFiles(repo);
        } catch (IOException | InterruptedException e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }

    public void extractJavaFiles(String repo) throws IOException, InterruptedException {
        System.out.println("Obtendo informações do repositório...");
        String branch = getDefaultBranch(repo);
        System.out.println("Branch padrão encontrada: " + branch);
        System.out.println("Preparando para baixar o arquivo ZIP...");
        Path zipPath = downloadRepositoryZip(repo, branch, this.timestamp);
        System.out.println("Arquivo ZIP salvo em: " + zipPath);
        System.out.println("Descompactando o arquivo ZIP...");
        Path tempDir = extractZip(zipPath);
        System.out.println("Descompactação concluída.");
        System.out.println("Iniciando a extração dos arquivos .java...");
        concatenateJavaFiles(tempDir);
        System.out.println("Arquivos .java extraídos e concatenados com sucesso.");
        System.out.println("Limpando arquivos temporários...");
        cleanup(zipPath, tempDir);
        System.out.println("Limpeza concluída.");
    }

    private String getDefaultBranch(String repo) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API_URL + repo))
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao acessar informações do repositório: " + response.statusCode());
        }

        JsonNode repoInfo = objectMapper.readTree(response.body());
        return repoInfo.get("default_branch").asText();
    }

    private Path downloadRepositoryZip(String repo, String branch, String timestamp) throws IOException, InterruptedException {
        String zipUrl = GITHUB_API_URL + repo + "/zipball/" + branch;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(zipUrl))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao baixar o ZIP: status " + response.statusCode());
        }

        Path zipPath = Files.createTempFile("repo_" + timestamp, ".zip");
        Files.copy(response.body(), zipPath, StandardCopyOption.REPLACE_EXISTING);
        return zipPath;
    }

    private Path extractZip(Path zipPath) throws IOException {
        Path tempDir = Files.createTempDirectory("repo");
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File newFile = newFile(tempDir.toFile(), entry);
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        return tempDir;
    }

    private void concatenateJavaFiles(Path tempDir) throws IOException {
        String outputFile = repo.replaceAll("[/\\\\:<>\"?*|]", "_") + "_" + this.timestamp + ".txt";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            Files.walk(tempDir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            String rawCode = Files.readString(p);
                            String cleanedCode = removeComments(rawCode);
                            String declaration = extractDeclaration(cleanedCode);
                            String packageName = extractPackageName(cleanedCode);

                            writer.write("// FILE: " + p.getFileName().toString());
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
                        } catch (IOException e) {
                            System.err.println("Erro lendo arquivo " + p + ": " + e.getMessage());
                        }
                    });
        }
    }

    private String removeComments(String code) {
        // Remove apenas blocos de comentários /* */
        return code.replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private String extractDeclaration(String code) {
        Matcher matcher = Pattern.compile("\\b(class|interface|enum|record|@interface)\\s+(\\w+)").matcher(code);
        return matcher.find() ? matcher.group(1) + " " + matcher.group(2) : "N/A";
    }

    private String extractPackageName(String code) {
        Matcher matcher = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE).matcher(code);
        return matcher.find() ? matcher.group(1) : "(default)";
    }

    private void cleanup(Path zipPath, Path tempDir) throws IOException {
        Files.deleteIfExists(zipPath);
        deleteDirectory(tempDir.toFile());
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        Files.deleteIfExists(directory.toPath());
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entrada ZIP fora do diretório destino: " + zipEntry.getName());
        }

        return destFile;
    }
}
