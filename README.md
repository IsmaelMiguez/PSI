# PSI
AppTTPSI

*Nota: Cuando lo abris se pone la pantalla en granate. Si se pone por mucho tiempo tocad en la zona izquierda de la pantalla más o menos por el centro. Pasa por que aveces no carga el menu no se porque.

*Nota: Para las pruebas use este movil Pixel_3a_API_34_extension_level_7_x86_64. Los layouts están adpatados para el hay que mirar de hacer que se adapten para todos,





# GitFlow: Metodología de Trabajo

Este repositorio utiliza **GitFlow**, una metodología de ramificación que facilita el trabajo en equipo, la integración continua y el control de versiones de forma ordenada y clara.

## 🌱 ¿Qué es GitFlow?

GitFlow define una estructura de ramas principal para organizar el desarrollo:

- `main`: contiene el código en producción.
- `develop`: contiene el código preparado para la próxima versión.
- `feature/*`: ramas para nuevas funcionalidades que se integran en `develop`.

- # 🚀 ¿Cómo trabajar con GitFlow? (Guía práctica)

A continuación, se detallan los pasos típicos para trabajar en una nueva funcionalidad siguiendo GitFlow.

### 1. Clonar el repositorio

Abrir el terminal CMD/Powershell o consola de Android estudio, navegar hasta la carpeta donde se quiere clonar el repositorio. En la carpeta elegida escribir:

```bash
git clone https://github.com/IsmaelMiguez/PSI.git
cd PSI
git branch
```

Así podremos comprobar que ramas hemos clonado en nuestro repositorio en local que por lo general es la rama principale del proyecto, la rama main.

### 2. Cambiar a la rama develop
Una vez clonado el repositorio, cambiamos a la rama develop, que ya existe en el repositorio remoto y es la base para crear nuevas funcionalidades.

```bash
git checkout develop
```

Si no tienes la rama develop en local, puedes traerla desde el remoto así:
```
git fetch origin
git checkout develop
```
Con esto ya estarás trabajando sobre la rama develop.

### 3. Crear una rama feature a partir de develop
Ahora creamos una nueva rama feature para trabajar en nuestra funcionalidad. El nombre debe ser descriptivo y seguir el formato feature/nombre-descriptivo (por ejemplo feature/pantalla_compartida o feature/pantalla_inicio)

```bash
git checkout -b feature/nombre-de-la-funcionalidad
```

Esta rama es donde trabajarás en los cambios sin afectar directamente a develop.

### 4. Commitear los cambios.
Después de hacer cambios en el código, se deben guardar (commitear) de la siguiente manera:

```bash
git add .
git commit -m "comentario indicando cambios"
git push origin feature/nombre de la rama
```

Intentad no subir la carpeta build, solo los archivos de código que cabieis. También podeis subir los archivos desde el android studio sin utilizar los comandos pero fijaos que los subis en la rama feature que creasteis

### 5.Actualizar rama 
Si mientras trabajas, otros compañeros han subido cambios a develop, deberías actualizarlos en tu rama feature.

Primero, asegúrate de tener los últimos cambios de develop:
```bash
git checkout develop
git pull origin develop
```

Luego, vuelve a tu rama feature y haz un merge con develop:
```bash
git checkout feature/login-usuario
git merge develop
```

Si se generan conflictos, edita los archivos en conflicto, luego guarda los cambios y haz commit:
```bash
git add .
git commit -m "resolución de conflictos con develop"
git push origin feature/nombre de la rama
```

### 7. Crar pull request
Cuando hayas terminado de desarrollar y probado la funcionalidad crea una pull request en github.Entra al repositorio de GitHub y crea un Pull Request de tu rama feature hacia develop.
