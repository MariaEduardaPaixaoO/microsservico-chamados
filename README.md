# chamados-service

Microserviço de chamados técnicos do ThinDesk, feito para a Prática M1 - Parte 2.

Ele roda separado do ThinDesk (que já tem o login), na porta 8081. A ideia é que mais pra frente o ThinDesk chame esse serviço depois que o usuário estiver logado.

Tecnologias: Java 17, Spring Boot 3 e MongoDB Atlas.

## Endpoints

- `POST /api/chamados` - abre um chamado
- `GET /api/chamados/usuario/{id}` - lista os chamados de um usuário
- `GET /api/chamados/{id}` - mostra um chamado
- `PUT /api/chamados/{id}/resolver` - técnico resolve o chamado

Como a autenticação fica no ThinDesk, o usuário logado é passado no header `X-Usuario-Id`. No POST ele vira o solicitante e no PUT ele vira o técnico. Se não mandar o header, volta 401.

Exemplo para abrir um chamado:

```
POST http://localhost:8081/api/chamados
X-Usuario-Id: usuario-maria

{
  "titulo": "Computador não liga",
  "descricao": "O computador da recepção não liga desde hoje cedo."
}
```

O próprio sistema preenche o solicitante, a data de abertura e o status (ABERTO).

Exemplo para resolver:

```
PUT http://localhost:8081/api/chamados/{id}/resolver
X-Usuario-Id: tecnico-joao

{
  "solucao": "Fonte trocada e equipamento testado."
}
```

Aí o status muda para RESOLVIDO e a data de resolução é registrada.

## Regras

- título: obrigatório, de 5 a 100 caracteres
- descrição e solução: obrigatórias, de 10 a 2000 caracteres
- chamado que já está RESOLVIDO não pode ser resolvido/alterado de novo (retorna 409)
- chamado que não existe retorna 404
- campo inválido retorna 400 com a mensagem do erro

## Como rodar

Precisa ter o Java 17 ou mais novo. Não precisa instalar o Maven, o projeto usa o `mvnw`.

A conexão com o banco vem da variável `MONGODB_URI`, para a senha do Atlas não ficar no código:

```bash
export MONGODB_URI="mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority"
./mvnw spring-boot:run
```

No Windows:

```powershell
$env:MONGODB_URI="mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority"
.\mvnw.cmd spring-boot:run
```

Os dados ficam no banco `thindesk_chamados`, coleção `chamados`.

## Testes

Na pasta `postman` tem a collection com todas as requisições. É só importar no Postman e rodar em ordem (a primeira já guarda o id do chamado para as outras).

Também tem testes automatizados, que não precisam do Atlas:

```bash
./mvnw test
```

## Estrutura

```
controller  -> endpoints
service     -> regras de negócio
repository  -> acesso ao MongoDB
model       -> entidade Chamado e enum de status
dto         -> dados que chegam nas requisições
exception   -> tratamento dos erros
```