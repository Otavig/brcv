# brcv

Ferramenta de linha de comando para trabalhar com Pix (BR Code). O tipo do Pix (estático ou dinâmico) é detectado automaticamente.

## Comandos

    brcv validate -p <payload> [-r]    # valida (true/false); -r mostra o tipo e o resultado por extenso
    brcv decode   -p <payload>         # EMV completo em JSON (tag, nome, tamanho, valor e subtags)
    brcv extract  -p <payload>         # resumo em JSON (tipo, chave/url, nome, cidade, valor, txid, crcValid)
    brcv fix-crc  -p <payload>         # recalcula o CRC e mostra o payload corrigido
    brcv create --key <chave> --name <nome> --city <cidade> [--amount 10,50] [--txid X] [--description X]
    brcv create --url <url>   --name <nome> --city <cidade> [--amount 10,50] [--txid X]

`--key` cria um Pix estático e `--url` cria um Pix dinâmico (use só um dos dois). Dica: coloque o payload entre aspas simples, por causa do `*` e dos espaços.

Exemplo:

    brcv extract -p '00020101021126430014br.gov.bcb.pix0115teste@email.com...'

## Instalar

- Windows: veja [INSTALL-WINDOWS.md](INSTALL-WINDOWS.md)
- Linux e macOS: veja [INSTALL-LINUX.md](INSTALL-LINUX.md)

## Compilar e rodar pelo código-fonte

Precisa de Java 21+ e Maven (o Maven não vem no projeto).

    # Windows (oferece instalar o Java e o Maven pelo Chocolatey, se faltar)
    .\build.ps1 validate -p '<payload>'

    # Linux e macOS
    ./build.sh validate -p '<payload>'

Só gerar o jar: `mvn package` (resultado em `target/brcv.jar`).
