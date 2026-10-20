# Instalação no Linux (e macOS)

## Requisitos
- Java 21 ou superior.
  - Verifique com: `java -version`
  - Se não tiver: instale o OpenJDK 21 pelo gerenciador de pacotes da sua distribuição, por exemplo `sudo apt install openjdk-21-jdk` no Ubuntu/Debian.

## Instalar
Dentro da pasta extraída do zip:

    chmod +x install.sh
    ./install.sh

O script verifica o Java, instala o programa em `~/.local/share/brcv` e cria o comando `brcv` em `~/.local/bin`.

## Testar
    brcv --version
    brcv --help

Se o comando não for encontrado, sua pasta `~/.local/bin` não está no PATH. Adicione esta linha ao `~/.bashrc` (ou `~/.zshrc`, se usa zsh) e abra um novo terminal:

    export PATH="$HOME/.local/bin:$PATH"

## Desinstalar
    ./install.sh --uninstall
