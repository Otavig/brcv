# brcv para Windows

Ferramenta de linha de comando para Pix (BR Code).

## Opção 1: Instalador `.exe` (recomendado, não precisa de Java)
1. Dê dois cliques em `brcv-<versão>-setup.exe` e clique em Avançar/Instalar.
2. Abra um **novo** PowerShell ou CMD e teste:

       brcv --version

Instala só para o seu usuário (sem Administrador) e já coloca o `brcv` no PATH.

## Opção 2: `install.bat` (precisa de Java 21+)
Dê dois cliques em `install.bat`. Ele instala o `brcv.jar` e cria o comando `brcv`. Abra um **novo** terminal e teste `brcv --version`.
Sem Java: `choco install temurin21 -y` ou baixe em https://adoptium.net.

## Sem instalar
    java -jar brcv.jar --help

## Usar
    brcv validate -p '<payload>' [-r]
    brcv decode   -p '<payload>'
    brcv extract  -p '<payload>'
    brcv fix-crc  -p '<payload>'
    brcv create --key <chave> --name <nome> --city <cidade> [--amount 10,50] [--txid X] [--description X]
    brcv create --url <url>   --name <nome> --city <cidade> [--amount 10,50] [--txid X]

No PowerShell use aspas simples no payload. O tipo (estático/dinâmico) é detectado sozinho.

## Desinstalar
- Exe: Configurações > Aplicativos > brcv > Desinstalar.
- install.bat: `powershell -ExecutionPolicy Bypass -File .\install.ps1 -Uninstall`
