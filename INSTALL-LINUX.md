# Instalação no Linux (e macOS)

## Requisitos
- Java 21 ou superior.
  - Verifique com: `java -version`
  - Se não tiver: instale o OpenJDK 21 pelo gerenciador de pacotes, por exemplo `sudo apt install openjdk-21-jdk` no Ubuntu/Debian ou `brew install openjdk@21` no macOS.
- Maven, **só se for compilar pelo código-fonte** (o Maven não vem no projeto). Se você recebeu a pasta com `brcv.jar`, não precisa.
  - Verifique com: `mvn -version`
  - Se não tiver: `sudo apt install maven` (Ubuntu/Debian) ou `brew install maven` (macOS).

## Compilar (só no código-fonte)
Dentro da pasta do projeto:

    chmod +x build.sh
    ./build.sh --help

O script compila com o Maven e roda o programa. O jar fica em `target/brcv.jar`.

## Instalar
Dentro da pasta (precisa do `target/brcv.jar`; se ele não existir, o script usa o Maven para gerar):

    chmod +x install.sh
    ./install.sh

O script verifica o Java, instala o programa em `~/.local/share/brcv` e cria o comando `brcv` em `~/.local/bin`.

## Testar
    brcv --version
    brcv --help

Se o comando não for encontrado, sua pasta `~/.local/bin` não está no PATH. Adicione esta linha ao `~/.bashrc` (ou `~/.zshrc`, se usa zsh) e abra um novo terminal:

    export PATH="$HOME/.local/bin:$PATH"

Os comandos (`validate`, `decode`, `extract`, `create` e `fix-crc`) estão no [README.md](README.md).

## Desinstalar
    ./install.sh --uninstall
