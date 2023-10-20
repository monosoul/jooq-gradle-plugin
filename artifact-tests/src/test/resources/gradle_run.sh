#!/bin/sh

gradle classes --info --stacktrace

export DOCKER_HOST=unix:///var/run/docker-alt.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker-alt.sock

gradle classes --info --stacktrace