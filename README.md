# JavaFilesExtractor

O **JavaFilesExtractor** é uma ferramenta em Java que extrai todos os arquivos `.java` de um repositório GitHub, concatenando-os em um único arquivo de texto com metadados e sem blocos de comentários `/* */`. Ideal para análise de código, auditoria ou geração de documentação de projetos Java, especialmente em contextos que envolvem processamento por algoritmos de inteligência artificial.

## Objetivo

O objetivo principal do **JavaFilesExtractor** é extrair código-fonte Java de repositórios GitHub e normalizá-lo para reduzir o ruído na entrada de algoritmos de Large Language Models (LLMs). Ao remover blocos de comentários `/* */` e estruturar o código com metadados claros (nome do arquivo, pacote e declaração), o projeto facilita o uso do código em tarefas como análise automatizada, geração de documentação ou treinamento de modelos de IA, garantindo uma entrada mais limpa e padronizada.

## Funcionalidades

- **Download Automático**: Baixa o ZIP da branch padrão de um repositório GitHub.
- **Extração de Arquivos**: Descompacta o ZIP e identifica todos os arquivos `.java`.
- **Concatenação com Metadados**: Gera um arquivo de texto contendo:
  - Nome do arquivo (`// FILE: NomeDoArquivo.java`).
  - Pacote declarado (`// PACKAGE: nome.do.pacote` ou `(default)`).
  - Declaração principal (`// DECLARATION: class Nome` ou similar).
  - Código-fonte sem blocos de comentários `/* */`.
  - Marca de fim (`// END_OF_FILE`).
- **Limpeza de Comentários**: Remove blocos de comentários `/* */`, preservando comentários de linha `//`.
- **Gerenciamento de Recursos**: Exclui arquivos temporários (ZIP e diretórios) após o uso.

## Pré-requisitos

- **Java 11 ou superior**: O projeto usa a API `HttpClient` e outras funcionalidades modernas.
- **Dependências**:
  - [Jackson Databind](https://github.com/FasterXML/jackson-databind) para parsing de JSON.
  - Inclua no seu arquivo pom.xml (Maven):

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.0</version>
</dependency>
```

## Como Usar

1. **Clone o repositório**:
   ```bash
   git clone https://github.com/seu-usuario/java-files-extractor.git
   cd java-files-extractor
   ```

2. **Importe e compile/rode o projeto (usa Maven) na sua IDE de preferencia**:
   Certifique-se de que o Jackson Databind está no classpath. Compile com:
   ```bash
   javac -cp .;lib/jackson-databind-2.15.2.jar src/main/java/*.java
   ```
3. **Insira o Repositório**:
   Quando solicitado, digite o repositório no formato `usuario/repositorio` (ex.: `rgiovann/pratt_parser`).

4. **Saída**:
   Um arquivo de texto (ex.: `usuario_repositorio_timestamp.txt`) será gerado no diretório atual, contendo todos os arquivos `.java` concatenados com metadados.

## Exemplo de Saída

Para um arquivo `App.java` no repositório `rgiovann/pratt_parser`:

```java
package parser.prat_parser;

public class App {
    public static void main(String[] args) {
        String input = "4+3*5 + 4/6 - (3+4)*7";
        PrattParser parser = new PrattParser(LexerFactory.createDefaultLexer(input));
        Expression parsed = parser.parse();
        System.out.println("INPUT : " + input);
        System.out.println("OUTPUT : " + parsed.toString());
    }
}
```

O arquivo de saída (`rgiovann_pratt_parser_20250509_123456.txt`) conterá:

```
// FILE: App.java
// PACKAGE: parser.prat_parser
// DECLARATION: class App

package parser.prat_parser;

public class App {
    public static void main(String[] args) {
        String input = "4+3*5 + 4/6 - (3+4)*7";
        PrattParser parser = new PrattParser(LexerFactory.createDefaultLexer(input));
        Expression parsed = parser.parse();
        System.out.println("INPUT : " + input);
        System.out.println("OUTPUT : " + parsed.toString());
    }
}
// END_OF_FILE
```

## Estrutura do Código

O projeto segue boas práticas de programação orientada a objetos, com ênfase nos princípios **SOLID**, **KISS** e **DRY**:

- **Single Responsibility Principle (SRP)**: Cada método tem uma única responsabilidade (ex.: `extractPackageName` extrai o pacote, `removeComments` remove blocos de comentários).
- **Open/Closed Principle (OCP)**: A lógica é extensível, permitindo adicionar novas funcionalidades (ex.: novas tags) sem modificar o núcleo.
- **Keep It Simple, Stupid (KISS)**: Soluções simples, como regex para parsing de pacotes e comentários, evitam complexidade desnecessária.
- **Don't Repeat Yourself (DRY)**: Funções reutilizáveis (ex.: `extractDeclaration`) evitam duplicação de código.

O código usa a API moderna do Java (`HttpClient`, `Files`, `Path`) para eficiência e robustez, com tratamento adequado de erros e recursos (ex.: fechamento automático com try-with-resources).

