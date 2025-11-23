# Changelog - DevReview v1.0

[+] Sistema de Auditoria (AuditManager) com logs assíncronos em arquivo e console.
[+] Motor de Regras (RulesEngine) para aprovação automática por horário e expiração de comandos.
[+] Categorização de Comandos (CategoryManager) com cores e prioridades na GUI (Critical, Moderation, Utility).
[+] Sistema de Justificativas obrigatórias via chat para aprovação/rejeição.
[+] Notificações Avançadas (NotificationManager) configuráveis (Chat, ActionBar, Title, Sound).
[+] Suporte a Banco de Dados SQL (MySQL/MariaDB) além do JSON padrão.
[+] Agendador de Tarefas (SchedulerManager) para execução periódica de comandos.
[+] Tradução completa para ES, DE, FR, RU, PL com novas chaves de notificação de bypass.
[+] Integração com bStats Metrics (com shading correto).
[+] Tarefa periódica para limpeza automática de comandos expirados.

[*] Thread Safety no JsonStagedCommandRepository usando coleções sincronizadas.
[*] Armazenamento de ID na GUI via PersistentDataContainer (removida dependência de Lore).
[*] Lógica de Bypass no CommandInterceptor com mensagens distintas para jogador e admins.
[*] LanguageManager agora suporta CommandSender (console) com fallback para inglês.
[*] Correção na estrutura do config.yml (remoção de duplicatas em critical-commands).
[*] Otimização de I/O: Logs de auditoria e operações de banco de dados agora são 100% assíncronos.

[-] Persistência de justificativas (agora são usadas apenas para notificação imediata).
[-] Configurações não utilizadas: multiple-approvals, cron, notify-before-seconds.
[-] Mensagens hardcoded no código (substituídas por chaves de linguagem).
[-] Código morto e importações não utilizadas em várias classes.
