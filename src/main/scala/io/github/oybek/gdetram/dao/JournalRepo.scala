package io.github.oybek.gdetram.dao

import io.github.oybek.gdetram.model.Record

trait JournalRepo[F[_]] {
  def insert(record: Record): F[Unit]
}
