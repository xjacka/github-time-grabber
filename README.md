# github-time-grabber

Github time grabber je prográmek, který si načte repozitáře z githubu na základě nastavené společnosti a z komentářů u issues přešte údaje o času stráveném jeho vyřešením. Tyto časy jsou označeny značkou :clock[0-9]:
Možné formáty jsou:
- 1h 30min
- 3h
- 15 min
- 1.5h

Aplikace má několik parametrů:
 1. github jméno uživatele, pro kterého se mají najít záznamy
 2. github token pro přístup k repozitáři
  - Token je potřeba nastavit zde https://github.com/settings/tokens a musí mít přístup na __repo__
 3. volitelně kolik dní nazpět se má brát
 4. volitelně kolik dní nazpět se má končit

např. github-time-grabber mojeJmeno tokenASDFA412ASDFASDF 2 1

Je možné spustit pomocí sbt:
$ sbt "run name token"
