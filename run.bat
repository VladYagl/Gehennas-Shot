for /r %%i in (*shaded.jar) do utils.set jar=%%i
java -jar %jar%
