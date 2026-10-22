# Instalação no Windows

Existem três formas. Escolha uma.

## Opção 1: Instalador `.exe` (sem precisar de Java)
Use se você recebeu o arquivo `brcv-1.0.0.exe`.

1. Dê dois cliques em `brcv-1.0.0.exe` e siga o assistente.
2. Abra um novo terminal (PowerShell ou CMD) e rode:

       brcv --version

O Java já vem incluído nesse instalador.

## Opção 2: Script `install.ps1` (precisa de Java)
Use se você recebeu a pasta com `brcv.jar` e `install.ps1`. O Maven **não** é necessário.

1. Tenha o Java 21 ou superior (veja a seção "Instalar o Java e o Maven" abaixo).
2. Abra o PowerShell dentro da pasta extraída e rode:

       powershell -ExecutionPolicy Bypass -File .\install.ps1

O script verifica o Java, instala o programa em `%LOCALAPPDATA%\brcv` e adiciona a pasta do comando ao PATH do seu usuário.

3. Abra um **novo** terminal (o PATH só vale depois de reabrir) e teste:

       brcv --version
       brcv --help

Para desinstalar:

    powershell -ExecutionPolicy Bypass -File .\install.ps1 -Uninstall

## Opção 3: Compilar pelo código-fonte (`build.ps1`)
Precisa de Java 21+ **e de Maven** (o Maven não vem no projeto, não existe `mvnw`).

    .\build.ps1 --help

O `build.ps1` compila e roda o programa. Se faltar o Java ou o Maven, ele pergunta se você quer instalar pelo Chocolatey: responda `y`. Para isso o PowerShell precisa estar aberto **como Administrador**.

Depois de compilar, você pode instalar o comando com a Opção 2.

## Instalar o Java e o Maven

### Pelo Chocolatey (recomendado)
O Chocolatey é um instalador de programas para o Windows.

1. Abra o PowerShell **como Administrador** (botão direito no PowerShell > Executar como administrador).
2. Instale o Chocolatey (comando oficial, de https://chocolatey.org/install):

       Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

3. Feche e abra o PowerShell como Administrador de novo e instale:

       choco install temurin21 -y
       choco install maven -y

4. Abra um **novo** terminal e confira:

       java -version
       mvn -version

### Manualmente (sem Chocolatey)
- Java: baixe o Temurin 21 em https://adoptium.net e instale.
- Maven:
  1. Baixe o "Binary zip archive" em https://maven.apache.org/download.cgi
  2. Extraia em uma pasta, por exemplo `C:\tools\apache-maven-3.9.9`
  3. Adicione `C:\tools\apache-maven-3.9.9\bin` ao PATH (Painel de Controle > Variáveis de ambiente > Path)
  4. Abra um novo terminal e rode `mvn -version`

## Usar

Os comandos (`validate`, `decode`, `extract`, `create` e `fix-crc`) estão no [README.md](README.md).
