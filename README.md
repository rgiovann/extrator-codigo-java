# Extrator de Código Java de Repositórios GitHub

Este projeto tem como objetivo facilitar a **análise de código-fonte Java por IAs**, extraindo automaticamente os arquivos `.java` relevantes de um repositório GitHub, removendo comentários e consolidando informações úteis como pacotes e declarações principais.

## Objetivo

O propósito é simplificar a leitura e a compreensão do código por ferramentas de inteligência artificial, consolidando trechos limpos, estruturados e identificáveis por metadados como:

* Caminho do arquivo

* Nome do pacote

* Tipo de declaração principal (classe, interface, enum etc.)

## Como funciona

O programa:

1. Solicita ao usuário o repositório GitHub (formato: `usuario/repositorio`)

2. Usa a [API REST do GitHub](https://docs.github.com/en/rest/git/trees?apiVersion=2022-11-28) para obter a árvore de arquivos da branch `main`

3. Filtra apenas os arquivos `.java` localizados em diretórios que contenham `src/`, ignorando pastas de teste

4. Baixa os arquivos `.java` diretamente do repositório (formato raw)

5. Remove os comentários de bloco

6. Extrai o tipo e nome da declaração principal do arquivo

7. Tenta inferir o nome do pacote com base no caminho

8. Escreve o conteúdo limpo e anotado em um único arquivo de saída `.txt`, nomeado com base no repositório e na data/hora da execução

***

## Exemplo de saída

```text
// FILE: src/main/java/com/exemplo/Exemplo.java
// PACKAGE: com.exemplo
// DECLARATION: class Exemplo

public class Exemplo {
    public void executar() {
        System.out.println("Executando...");
    }
}

// END_OF_FILE
```

***

## Pré-requisitos

* **Java 21** instalado e configurado\
  O projeto utiliza recursos modernos da plataforma Java, incluindo:

  * `java.net.http.HttpClient` (API de cliente HTTP assíncrono)

  * `java.nio.file.Files` e `Paths` para manipulação de arquivos

  * `java.time.LocalDateTime` para geração de timestamp

  * `Pattern` e `Matcher` para expressões regulares

  * Leitura JSON com [Jackson Databind](https://github.com/FasterXML/jackson-databind)

***

## Estrutura dos arquivos

### `GitHubJavaFileFetcherComNormalizacao.java`

Classe principal que executa todo o processo, desde a requisição à API do GitHub até a gravação dos arquivos extraídos. Responsável por:

* Buscar os arquivos `.java`

* Baixar o conteúdo original bruto (raw)

* Remover comentários

* Extrair declaração principal e nome do pacote

* Escrever o conteúdo limpo em um arquivo consolidado

***

## Compilação e execução

1. Clone o repositório:

   ```bash
   git clone https://github.com/rgiovann/extrator-codigo-java.git
   cd extrator-codigo-java
   ```

2. Compile o código:

   ```bash
   javac -cp "libs/*" github/exporter/GitHubJavaFileFetcherComNormalizacao.java
   ```

3. Execute:

   ```bash
   java -cp ".:libs/*" github.exporter.GitHubJavaFileFetcherComNormalizacao
   ```

