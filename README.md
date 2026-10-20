# Instalar

## Gerando o jar pelo maven
    mvn package

## Realizar a instalação do comando `brcv`
    mkdir -p ~/.local/share/brcv ~/.local/bin
    cp target/brcv.jar ~/.local/share/brcv/
    cp install/brcv ~/.local/bin/
    brcv --version

Se `~/.local/bin` não estiver no PATH, adicione ao seu shell:
    export PATH="$HOME/.local/bin:$PATH"

## Desenvolvimento sem Maven
    ./build.sh
