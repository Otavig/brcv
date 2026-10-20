# Instalação no Windows

Existem duas formas. Escolha uma.

## Opção 1: Instalador `.exe` (sem precisar de Java)
Use se você recebeu o arquivo `brcv-1.0.0.exe`.

1. Dê dois cliques em `brcv-1.0.0.exe` e siga o assistente.
2. Abra um novo terminal (PowerShell ou CMD) e rode:

       brcv --version

O Java já vem incluído nesse instalador.

## Opção 2: Script `install.ps1` (precisa de Java)
Use se você recebeu a pasta com `brcv.jar` e `install.ps1`.

### Requisitos
- Java 21 ou superior.
  - Verifique com: `java -version`
  - Se não tiver: baixe o Temurin 21 em https://adoptium.net e instale.

### Instalar
Abra o PowerShell dentro da pasta extraída e rode:

    powershell -ExecutionPolicy Bypass -File .\install.ps1

O script verifica o Java, instala o programa em `%LOCALAPPDATA%\brcv` e adiciona a pasta do comando ao PATH do seu usuário.

### Testar
Abra um **novo** terminal (o PATH só vale depois de reabrir) e rode:

    brcv --version
    brcv --help

### Desinstalar
    powershell -ExecutionPolicy Bypass -File .\install.ps1 -Uninstall
