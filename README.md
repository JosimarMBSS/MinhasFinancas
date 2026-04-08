# Minhas Finanças

Aplicativo Android nativo em Java para controle financeiro pessoal.

## Objetivo
Oferecer uma experiência simples, bonita e profissional para registrar receitas, despesas, categorias, recorrências e relatórios, com funcionamento offline.

## Base desta versão
- Android nativo em **Java**
- Interface em **XML + Material Design**
- Banco local em **SQLite direto** com `SQLiteOpenHelper`
- Projeto preparado para gerar APK pelo **GitHub Actions**

## Funcionalidades presentes
- Dashboard com saldo, receitas e despesas do mês
- Cadastro de receitas e despesas
- Categorias personalizadas
- Recorrências com geração automática de lançamentos
- Relatórios com filtros
- Tema claro/escuro

## Como gerar APK no GitHub
1. Suba este projeto para um repositório no GitHub.
2. Vá em **Actions**.
3. Execute o workflow **Build Debug APK**.
4. Ao final, baixe o artefato **minhas-financas-debug-apk**.

## Status
Base funcional pronta para evolução.


## Atualização 1.6.0
- totais reposicionados abaixo do seletor de mês
- formulário de lançamento redesenhado em blocos
- seletor visual para despesa ou receita
- status pago/recebido ajustado conforme o tipo


Versão atual do projeto: 1.9.0
