#include <linux/module.h> 		// para dev/
#include <linux/kernel.h> 		
#include <linux/init.h>			// init
#include <linux/miscdevice.h> 	// misc dev 
#include <linux/fs.h>			// file operations, (libraries linux)
#include <linux/uaccess.h>		// codigo para user 
#include <linux/wait.h>			// waiting queue
#include <linux/sched.h>		// TASK_INTERRUMPIBLE 
#include <linux/interrupt.h> 
#include <linux/timer.h>		// para usar timers
#include <linux/gpio.h> 		// utilities
#include <linux/jiffies.h>		// for time
#include <linux/delay.h>		// delay 

#define DRIVER_AUTHOR "Diego Alejandro Machaca (DAMH)"
#define DRIVER_DESC	"Driver Leds. DAC Rpi"

// CONFIGURATION PORTS: GPIOs
//#define GPIO_BUTTON1 2
//#define GPIO_BUTTON2 3

#define GPIO_SPEAKER 	4

#define GPIO_GREEN1 	27
#define GPIO_GREEN2 	22
#define GPIO_YELLOW1 	17
#define GPIO_YELLOW2 	11
#define GPIO_RED1		10
#define GPIO_RED2		9

#define HIGH			1
#define LOW				0

static int LED_GPIOS[]= {GPIO_GREEN1, GPIO_GREEN2, GPIO_YELLOW1, GPIO_YELLOW2, GPIO_RED1, GPIO_RED2} ;

 
static char *led_desc[]= {"GPIO_GREEN1","GPIO_GREEN2","GPIO_YELLOW1","GPIO_YELLOW2","GPIO_RED1","GPIO_RED 2"} ;


/****************************************************************************/
/* LEDs write/read using gpio kernel API	*/
/****************************************************************************/

static char leds2byte(void)
{
	int val; 
	char ch; 
	int i; 
	ch=0;
 
	for(i=0; i<6; i++){
		val=gpio_get_value(LED_GPIOS[i]); 
		ch |= (val << i);
	}
	return ch;
}

static void byte2leds(char ch){ 
	int i;
	int val = (int) ch;
	int bit = (int) leds2byte();

/*	Si el bit 7 y 6 están a cero: 00?????? 	-> los 6 leds tomaran el valor de salida con el bit dado: 1 encender y 0 apagar.
	Si el bit 6 está a 1: 		  01?????? 	-> si alguno de 6 leds tiene el bit a 1, entonces los leds estarán encendidos (ON)
	Si en bit 7 está a 1:	      10?????? 	-> si alguno de 6 leds tiene el bit a 0, entonces los leds estarán apagados (OFF)
	Si el bit 6 y 7 estan 1: 	  undefined. 
*/
	// con >> recoremos a la derecha y << a la izquierda
	if(val>>7 == 0 && val>>6 == 0){ 
		for(i=0; i<6; i++){
			gpio_set_value(LED_GPIOS[i], (val >> i) & 1);
		}
	}

	if(val>>6 == 1){
		val |= bit;
		for(i=0; i<6; i++){
			gpio_set_value(LED_GPIOS[i], (val >> i) & 1);
		}
	}

	if(val>>7 == 1){
		int aux = ~val;
		val = aux & bit;
		for(i=0; i<6; i++){
			gpio_set_value(LED_GPIOS[i], (val >> i) & 1);
		}
	}
}

/****************************************************************************/
/* LEDs device file operations	*/
/****************************************************************************/

static ssize_t leds_write(struct file *file, const char __user *buf,size_t count, loff_t *ppos)
{
	char ch;
	if (copy_from_user(&ch,buf,1)) { 
		return -EFAULT;
	}
	printk( KERN_INFO " (write) valor recibido: %d\n",(int)ch);
	byte2leds(ch); 
	
	if(ch == '0'){				//OFF
		gpio_set_value(GPIO_SPEAKER, LOW);
	}else{						//ON
		gpio_set_value(GPIO_SPEAKER, HIGH);	
	}	
	return 1;
}

static ssize_t leds_read(struct file *file, char __user *buf, size_t count, loff_t *ppos)
{
	char ch;
	if(*ppos==0){ 
		*ppos+=1;
	}else{
		 return 0;
	 }
	ch=leds2byte();
	
	printk( KERN_INFO " (read) valor entregado: %d\n",(int)ch); 
	
	if(copy_to_user(buf,&ch,1)){	
		return -EFAULT;
	}
	return 1;
}

/***********************************************************************
 * 				STRUCTURE FOR OPERATIONS LEDS
 * *********************************************************************/
static const struct file_operations leds_fops = {
	.owner = THIS_MODULE,
	.write = leds_write,
	.read = leds_read
};



/***********************************************************************
* 					LEDs device struct	
************************************************************************/

static struct miscdevice leds_miscdev = {
	.minor = MISC_DYNAMIC_MINOR,
	.name = "leds",
	.fops = &leds_fops,
	.mode	= S_IRUGO | S_IWUGO				//macro para lectura y escritura
};

/*****************************************************************************/
/* This functions registers devices, requests GPIOs and configures interrupts */
/*****************************************************************************/

/*******************************
* register device for leds 
*******************************/

static int r_dev_config(void)
{
	int ret=0;
	ret = misc_register(&leds_miscdev); 
	if (ret < 0) {
		printk(KERN_ERR "misc_register failed\n");
	}
 	else{
 	 	printk(KERN_NOTICE "misc_register OK... leds_miscdev.minor=%d\n", leds_miscdev.minor);
	}
 	return ret;
}

/***********************************************************************
* 					request and init gpios for leds
***********************************************************************/

static int r_GPIO_config(void)
{
	int i; 
	int res=0;
	for(i=0; i<6; i++)
	{
		if ((res=gpio_request_one(LED_GPIOS[i], GPIOF_INIT_LOW, led_desc[i]))){
			printk(KERN_ERR "GPIO request faiure: led GPIO %d %s\n",LED_GPIOS[i], led_desc[i]); return res;
		}
		gpio_direction_output(LED_GPIOS[i],0);
		//gpio_request_one(GPIO_SPEAKER, GPIOF_INIT_LOW, "gpio_speaker");
 	}
 	return res;
}

/****************************************************************************/
/* Module init / cleanup block.	*/
/****************************************************************************/

static void r_cleanup(void) { int i;
	printk(KERN_NOTICE "%s module cleaning up...\n", KBUILD_MODNAME); 
	for(i=0; i<6; i++){
		gpio_free(LED_GPIOS[i]);	
	}					//liberamos leds
	if (leds_miscdev.this_device){
		 misc_deregister(&leds_miscdev); 
	}
	printk(KERN_NOTICE "Done. Bye from %s module\n", KBUILD_MODNAME);
	// free speaker
	//gpio_free(GPIO_SPEAKER);
	
}
 


static int r_init(void) {
 	int res=0;
	printk(KERN_NOTICE "Hello, loading %s module!\n", KBUILD_MODNAME); 
	printk(KERN_NOTICE "%s - devices config...\n", KBUILD_MODNAME);
	printk(KERN_NOTICE "Init: %s ... \n" , DRIVER_DESC);
	if((res = r_dev_config())){
 	 	r_cleanup();
 	 	return res;
 	}
	printk(KERN_NOTICE "%s - GPIO config...\n", KBUILD_MODNAME);
	if((res = r_GPIO_config())){
 	 	r_cleanup();
 	 	return res;
 	}
	return res;
}

module_init(r_init); 
module_exit(r_cleanup);


/****************************************************************************/
/* Module licensing/description block.	*/
/****************************************************************************/ 
MODULE_LICENSE("GPL");
MODULE_AUTHOR(DRIVER_AUTHOR); 
MODULE_DESCRIPTION(DRIVER_DESC);
