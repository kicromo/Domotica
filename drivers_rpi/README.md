# Driver SO #

### Practica para controlar un berryclip a través de un driver###

* Un dirver normalemente funciona como un intermediario entre en hardware 
* y software, permitiendo al software controlar dicho hardware

### Vamos a tener 3 drivers: LEDS, SPEAKER, BOTONES ###
- El driver para los leds es bastante simple:
  Para poder controlor los 6 LEDs disponemos de un Modulo
  que registra nuestra actividad de escritura y lectura

Uso LEDS:
	/*	Si el bit 7 y 6 están a cero: 00?????? 	-> los 6 leds tomaran el valor de salida con el bit dado: 1 encender y 0 apagar.
	Si el bit 6 está a 1: 		  01?????? 	-> si alguno de 6 leds tiene el bit a 1, entonces los leds estarán encendidos (ON)
	Si en bit 7 está a 1:	      10?????? 	-> si alguno de 6 leds tiene el bit a 0, entonces los leds estarán apagados (OFF)
	Si el bit 6 y 7 estan 1: 	  undefined. 
*/
	usaremos ./bin2char para escribir sobre el modulo y ./dev2char para 
	leer sobre dicho contenido del modulo
	EJ:
		./bin2char 01110000 > /dev/leds      se encienden los 2 leds rojos
		./dev2char /dev/leds*                muestra el contenifo: 01110000

- El driver que tenemos para el speaker simplemente pone a 1 o 0 el modulo
  Eso quiere decir que si esta a 1, es porque lleva corriente (5V) y 0 
  cuando tiene (0V)
  para el uso de este driver disponemos de un fichero sonidoTetris.c, 
  que tras ejecutarlo con gcc tocará dicha melodia
  
  En cuanto al modulo es el mismo de antes
  EJ:
		./bin2char 1 > /dev/speaker			 speaker ON
		./dev2char /dev/leds*                muestra el contenifo: 1

- BOTONES:
* *fallo al pulsar (incomplete)

### Makefile ###

Use: 

* *make* para construir nuestro modulo

* *make install*, instala nuestro modulo

* *make uninstall*, desintala nuestro modulo

* *clean_driver, para limpiar el resto del codigo tras la instalacion
	(lo recomedable es desintalar antes el modulo para usar clean_driver)

EJ:
	make MODULE=driver_leds 

