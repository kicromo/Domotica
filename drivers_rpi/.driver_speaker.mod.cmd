cmd_/home/kicromo/SistemaOperativo/drivers_rpi/driver_speaker.mod := printf '%s\n'   driver_speaker.o | awk '!x[$$0]++ { print("/home/kicromo/SistemaOperativo/drivers_rpi/"$$0) }' > /home/kicromo/SistemaOperativo/drivers_rpi/driver_speaker.mod
