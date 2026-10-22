# brcv para Linux

Ferramenta de linha de comando para Pix (BR Code).

## Instalar (30 segundos)
Dentro desta pasta:

    ./install.sh

Abra um novo terminal e teste:

    brcv --version

Não precisa instalar Java: a pasta `brcv-app` já traz tudo. Se ela não existir, o `install.sh` usa o `brcv.jar` e precisa de Java 21+.

## Sem instalar
    java -jar brcv.jar --help      # precisa de Java 21+
    ./brcv-app/bin/brcv --help     # não precisa de Java

## Usar
    brcv validate -p '<payload>' [-r]
    brcv decode   -p '<payload>'
    brcv extract  -p '<payload>'
    brcv fix-crc  -p '<payload>'
    brcv create --key <chave> --name <nome> --city <cidade> [--amount 10,50] [--txid X] [--description X]
    brcv create --url <url>   --name <nome> --city <cidade> [--amount 10,50] [--txid X]

Use aspas simples no payload (por causa do `*` e dos espaços). O tipo (estático/dinâmico) é detectado sozinho.

## Desinstalar
    ./install.sh --uninstall
