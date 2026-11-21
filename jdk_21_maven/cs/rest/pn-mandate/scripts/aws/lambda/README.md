
##Istruzioni per la compilazione

In questa cartella sono stati inseriti gli script per estrarre il codice della lambda, 
zipparlo e deployarlo in localstack.
Si è scelto di non legarlo alla fase di compile.

Per i sistemi ***Unix-like***, si può tranquillamente lanciare il comando

```
    ./extract-zip-deploy-lambda.sh
```

Per i sistemi ***Windows*** invece non essendo disponibile il comando "zip", si è utilizzata 
la build Ant per invocare gli script, zippare e deployare.
Il comando ovviamente può essere usato anche in unix.
Per windows si può usare

```
    mvn antrun:run
```

***NB: richiede l'installazione di bash, come ad esempio quello presente in Git.
Di default è configurato il path: "c:\Program Files\Git\bin\bash.exe".***

Nel caso in cui il comando di bash sia installato in un altra posizione, si può passare da riga di comando, es:

```
    mvn antrun:run  "-Dant.windows_bash_exe=D:\path\bash.exe"
```

