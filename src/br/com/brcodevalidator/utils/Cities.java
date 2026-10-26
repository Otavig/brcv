package br.com.brcodevalidator.utils;

import java.util.ArrayList;
import java.util.List;

public final class Cities {

    public record City(String name, String uf) {
        public String pixName() {
            return Text.ascii(name).toUpperCase();
        }
    }

    private static final String DATA = """
        AC|Rio Branco,Cruzeiro do Sul,Sena Madureira,Tarauacá,Feijó
        AL|Maceió,Arapiraca,Palmeira dos Índios,Rio Largo,União dos Palmares,Penedo,Delmiro Gouveia,São Miguel dos Campos
        AP|Macapá,Santana,Laranjal do Jari,Oiapoque,Mazagão
        AM|Manaus,Parintins,Itacoatiara,Manacapuru,Coari,Tabatinga,Tefé,Maués
        BA|Salvador,Feira de Santana,Vitória da Conquista,Camaçari,Itabuna,Juazeiro,Lauro de Freitas,Ilhéus,Jequié,Teixeira de Freitas,Barreiras,Alagoinhas,Porto Seguro,Simões Filho,Paulo Afonso,Eunápolis,Santo Antônio de Jesus,Valença,Candeias,Guanambi,Jacobina,Serrinha,Senhor do Bonfim,Luís Eduardo Magalhães,Irecê,Bom Jesus da Lapa,Brumado
        CE|Fortaleza,Caucaia,Juazeiro do Norte,Maracanaú,Sobral,Crato,Itapipoca,Maranguape,Iguatu,Quixadá,Pacatuba,Aquiraz,Canindé,Russas,Crateús,Tianguá,Eusébio
        DF|Brasília
        ES|Vitória,Vila Velha,Serra,Cariacica,Linhares,Cachoeiro de Itapemirim,São Mateus,Colatina,Guarapari,Aracruz,Viana,Nova Venécia,Barra de São Francisco,Marataízes
        GO|Goiânia,Aparecida de Goiânia,Anápolis,Rio Verde,Luziânia,Águas Lindas de Goiás,Valparaíso de Goiás,Trindade,Formosa,Novo Gama,Itumbiara,Senador Canedo,Catalão,Jataí,Planaltina,Caldas Novas,Santo Antônio do Descoberto,Goianésia,Cidade Ocidental,Mineiros
        MA|São Luís,Imperatriz,São José de Ribamar,Timon,Caxias,Codó,Paço do Lumiar,Açailândia,Bacabal,Balsas,Santa Inês,Pinheiro,Chapadinha,Barra do Corda
        MT|Cuiabá,Várzea Grande,Rondonópolis,Sinop,Tangará da Serra,Cáceres,Sorriso,Lucas do Rio Verde,Primavera do Leste,Barra do Garças,Alta Floresta,Nova Mutum,Campo Verde,Pontes e Lacerda
        MS|Campo Grande,Dourados,Três Lagoas,Corumbá,Ponta Porã,Naviraí,Nova Andradina,Sidrolândia,Maracaju,Aquidauana,Paranaíba,Coxim,Rio Brilhante
        MG|Belo Horizonte,Uberlândia,Contagem,Juiz de Fora,Betim,Montes Claros,Ribeirão das Neves,Uberaba,Governador Valadares,Ipatinga,Sete Lagoas,Divinópolis,Santa Luzia,Ibirité,Poços de Caldas,Patos de Minas,Pouso Alegre,Teófilo Otoni,Barbacena,Sabará,Varginha,Conselheiro Lafaiete,Vespasiano,Itabira,Araguari,Passos,Ubá,Lavras,Muriaé,Ituiutaba,Itajubá,Araxá,Caratinga,São João del-Rei,Nova Lima,Curvelo,Ouro Preto,Diamantina,Paracatu,Patrocínio,Unaí,Três Corações,Guaxupé,Alfenas,Manhuaçu,Formiga,Pará de Minas,Lagoa Santa,Timóteo,Cataguases,Itaúna,Janaúba,Januária,Bom Despacho,Mariana,Congonhas,Campo Belo,Viçosa,São Lourenço,Caxambu
        PA|Belém,Ananindeua,Santarém,Marabá,Parauapebas,Castanhal,Abaetetuba,Cametá,Marituba,Bragança,Altamira,Tucuruí,Barcarena,Paragominas,Redenção,Itaituba,Capanema,Tailândia,Breves,Benevides
        PB|João Pessoa,Campina Grande,Santa Rita,Patos,Bayeux,Sousa,Cabedelo,Cajazeiras,Guarabira,Mamanguape,Sapé,Pombal,Monteiro,Esperança
        PR|Curitiba,Londrina,Maringá,Ponta Grossa,Cascavel,São José dos Pinhais,Foz do Iguaçu,Colombo,Guarapuava,Paranaguá,Araucária,Toledo,Apucarana,Pinhais,Campo Largo,Almirante Tamandaré,Umuarama,Paranavaí,Cambé,Sarandi,Fazenda Rio Grande,Piraquara,Cianorte,Telêmaco Borba,Campo Mourão,Pato Branco,Francisco Beltrão,Arapongas,Rolândia,Castro,Irati,União da Vitória,Lapa,Palmas,Guaratuba,Matinhos
        PE|Recife,Jaboatão dos Guararapes,Olinda,Caruaru,Petrolina,Paulista,Cabo de Santo Agostinho,Camaragibe,Garanhuns,Vitória de Santo Antão,Igarassu,São Lourenço da Mata,Santa Cruz do Capibaribe,Abreu e Lima,Ipojuca,Serra Talhada,Araripina,Gravatá,Carpina,Goiana,Belo Jardim,Arcoverde,Ouricuri,Escada,Pesqueira,Surubim,Bezerros
        PI|Teresina,Parnaíba,Picos,Piripiri,Floriano,Campo Maior,Barras,União,Altos,Pedro II,Oeiras,São Raimundo Nonato,Corrente
        RJ|Rio de Janeiro,São Gonçalo,Duque de Caxias,Nova Iguaçu,Niterói,Belford Roxo,São João de Meriti,Campos dos Goytacazes,Petrópolis,Volta Redonda,Magé,Itaboraí,Mesquita,Nova Friburgo,Barra Mansa,Macaé,Cabo Frio,Nilópolis,Queimados,Angra dos Reis,Teresópolis,Resende,Maricá,Itaguaí,Araruama,Japeri,Três Rios,Paracambi,São Pedro da Aldeia,Rio das Ostras,Armação dos Búzios,Paraty,Valença,Barra do Piraí,Itaperuna,Saquarema,Seropédica
        RN|Natal,Mossoró,Parnamirim,São Gonçalo do Amarante,Macaíba,Ceará-Mirim,Caicó,Currais Novos,São José de Mipibu,Santa Cruz,Nova Cruz,Apodi
        RS|Porto Alegre,Caxias do Sul,Canoas,Pelotas,Santa Maria,Gravataí,Viamão,Novo Hamburgo,São Leopoldo,Rio Grande,Alvorada,Passo Fundo,Sapucaia do Sul,Uruguaiana,Santa Cruz do Sul,Cachoeirinha,Bagé,Bento Gonçalves,Erechim,Guaíba,Cachoeira do Sul,Santana do Livramento,Esteio,Ijuí,Alegrete,Lajeado,Santo Ângelo,Sapiranga,Farroupilha,Venâncio Aires,Camaquã,Vacaria,Montenegro,Santa Rosa,Carazinho,Frederico Westphalen,Gramado,Canela,Torres,Cruz Alta,São Borja,Capão da Canoa,Tramandaí
        RO|Porto Velho,Ji-Paraná,Ariquemes,Vilhena,Cacoal,Jaru,Rolim de Moura,Guajará-Mirim,Pimenta Bueno,Ouro Preto do Oeste,Buritis
        RR|Boa Vista,Rorainópolis,Caracaraí,Cantá,Pacaraima,Mucajaí
        SC|Joinville,Florianópolis,Blumenau,São José,Chapecó,Itajaí,Criciúma,Jaraguá do Sul,Palhoça,Lages,Balneário Camboriú,Brusque,Tubarão,São Bento do Sul,Camboriú,Navegantes,Caçador,Concórdia,Rio do Sul,Araranguá,Gaspar,Biguaçu,Indaial,Içara,Mafra,Canoinhas,Itapema,Videira,Xanxerê,Jaguaruna,Imbituba,Laguna,Joaçaba,Fraiburgo,Pomerode,Timbó,São Francisco do Sul,Garopaba,Bombinhas
        SP|São Paulo,Guarulhos,Campinas,São Bernardo do Campo,Santo André,Osasco,São José dos Campos,Ribeirão Preto,Sorocaba,Mauá,São José do Rio Preto,Santos,Mogi das Cruzes,Diadema,Jundiaí,Piracicaba,Carapicuíba,Bauru,Itaquaquecetuba,São Vicente,Franca,Praia Grande,Guarujá,Taubaté,Limeira,Suzano,Taboão da Serra,Sumaré,Barueri,Embu das Artes,São Carlos,Marília,Indaiatuba,Cotia,Americana,Jacareí,Araraquara,Presidente Prudente,Hortolândia,Rio Claro,Araçatuba,Ferraz de Vasconcelos,Francisco Morato,Itapevi,Itu,Bragança Paulista,Pindamonhangaba,Itapecerica da Serra,São Caetano do Sul,Franco da Rocha,Mogi Guaçu,Jaú,Botucatu,Atibaia,Santana de Parnaíba,Araras,Catanduva,Birigui,Sertãozinho,Ourinhos,Assis,Votorantim,Lins,Tatuí,Barretos,Guaratinguetá,Cubatão,Valinhos,Caraguatatuba,Ubatuba,Ilhabela,São Sebastião,Bertioga,Itanhaém,Peruíbe,Mongaguá,Registro,Avaré,Lorena,Cruzeiro,Jales,Fernandópolis,Votuporanga,Tupã,Dracena,Adamantina,Ibitinga,Matão,Leme,Mococa,São João da Boa Vista,Poá,Ribeirão Pires,Arujá,Mairiporã,Caieiras,Cajamar,Vinhedo,Paulínia,Nova Odessa,Salto,Porto Feliz,Mairinque,Amparo,Socorro,Serra Negra,Campos do Jordão,Cravinhos
        SE|Aracaju,Nossa Senhora do Socorro,Lagarto,Itabaiana,São Cristóvão,Estância,Tobias Barreto,Capela,Simão Dias,Propriá
        TO|Palmas,Araguaína,Gurupi,Porto Nacional,Paraíso do Tocantins,Colinas do Tocantins,Guaraí,Tocantinópolis,Dianópolis,Formoso do Araguaia,Miracema do Tocantins
        """;

    private static final List<City> ALL = load();

    private Cities() {
        throw new IllegalStateException("The Cities class cannot/should not be instantiated");
    }

    private static List<City> load() {
        List<City> list = new ArrayList<>();
        for (String line : DATA.strip().split("\n")) {
            String[] parts = line.strip().split("\\|");
            for (String name : parts[1].split(",")) {
                list.add(new City(name.strip(), parts[0]));
            }
        }
        return List.copyOf(list);
    }

    public static List<City> all() {
        return ALL;
    }

    public static boolean isState(String uf) {
        return ALL.stream().anyMatch(c -> c.uf().equalsIgnoreCase(uf));
    }

    public static List<City> filter(String uf, boolean pixSize) {
        List<City> result = new ArrayList<>();
        for (City c : ALL) {
            if (uf != null && !c.uf().equalsIgnoreCase(uf)) {
                continue;
            }
            if (pixSize && c.pixName().length() > 15) {
                continue;
            }
            result.add(c);
        }
        return result;
    }
}
