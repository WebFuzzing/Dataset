#!/bin/sh

case "${JASPER_GC}" in
	zgc)
		exec java \
			-Djava.security.egd=file:/dev/./urandom \
			${JASPER_CORES:+-XX:ActiveProcessorCount=${JASPER_CORES}} \
			"-Xms${JASPER_HEAP:-512m}" \
			"-Xmx${JASPER_HEAP:-512m}" \
			-XX:+UseCompactObjectHeaders \
			-XX:+UseZGC \
			-XX:+UseStringDeduplication \
			org.springframework.boot.loader.launch.JarLauncher
		;;

	parallel)
		exec java \
			-Djava.security.egd=file:/dev/./urandom \
			${JASPER_CORES:+-XX:ActiveProcessorCount=${JASPER_CORES}} \
			"-Xms${JASPER_HEAP:-512m}" \
			"-Xmx${JASPER_HEAP:-512m}" \
			-XX:+UseCompactObjectHeaders \
			-XX:+UseParallelGC \
			org.springframework.boot.loader.launch.JarLauncher
		;;

	g1gc | *)
		exec java \
			-Djava.security.egd=file:/dev/./urandom \
			${JASPER_CORES:+-XX:ActiveProcessorCount=${JASPER_CORES}} \
			"-Xms${JASPER_HEAP:-512m}" \
			"-Xmx${JASPER_HEAP:-512m}" \
			-XX:+UseCompactObjectHeaders \
			-XX:+UseG1GC \
			-XX:+UseStringDeduplication \
			org.springframework.boot.loader.launch.JarLauncher
		;;
esac
