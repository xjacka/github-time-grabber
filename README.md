# github-time-grabber

Github time grabber je prográmek, který si načte repozitáře z githubu na základě nastavené společnosti a z komentářů u issues přešte údaje o času stráveném jeho vyřešením. Tyto časy jsou označeny značkou :clock[0-9]:
Možné formáty jsou:
- 1h 30min
- 3h
- 15 min
- 1.5h

### Parametry aplikace
 1. název společnosti na githubu ze které se berou repozitáře
 2. github jméno uživatele, pro kterého se mají najít záznamy
 3. github token pro přístup k repozitáři
  - Token je potřeba nastavit zde https://github.com/settings/tokens a musí mít přístup na __repo__
 4. volitelně kolik dní nazpět se má brát
 5. volitelně kolik dní nazpět se má končit

### Spuštění
[Stáhněte](https://github.com/xjacka/github-time-grabber/releases/download/0.2/github-time-grabber.jar) si jar archiv ze stránky [releases](https://github.com/xjacka/github-time-grabber/releases) a spusťte ho:    
např.    
`$ java -jar github-time-grabber.jar spolecnost jmeno token`    
nebo    
`$ java -jar github-time-grabber.jar spolecnost jmeno token 2 1`

Je možné spustit pomocí sbt:
$ sbt "run spolecnost name token"
