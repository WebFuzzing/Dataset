#!/usr/bin/env Rscript
# http-verbs.R
# Functions to convert http-verbs.csv into LaTeX and Markdown tables.
#
# Usage:
#   source("http-verbs.R")
#   latex()      # writes http-verbs.tex
#   markdown()   # writes http-verbs.md


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

# Read the CSV, coerce counts to numeric, add a per-row Total column and
# a bottom "Total" row. Returns a data frame ready to be rendered.
.build_table <- function(csv_path = "http-verbs.csv") {
  if (!file.exists(csv_path)) {
    stop("Could not find ", csv_path)
  }

  df <- read.csv(csv_path, check.names = FALSE, stringsAsFactors = FALSE)

  verb_cols <- setdiff(names(df), "API")
  for (col in verb_cols) {
    df[[col]] <- as.numeric(df[[col]])
  }

  # Per-row total column
  df$Total <- rowSums(df[verb_cols], na.rm = TRUE)

  # Bottom Total row
  col_sums <- colSums(df[verb_cols], na.rm = TRUE)
  grand_total <- sum(col_sums)

  total_row <- as.data.frame(as.list(col_sums), check.names = FALSE)
  total_row <- cbind(
    data.frame(API = "Total", check.names = FALSE),
    total_row,
    data.frame(Total = grand_total, check.names = FALSE)
  )
  total_row <- total_row[, names(df), drop = FALSE]

  out <- rbind(df, total_row)
  for (col in c(verb_cols, "Total")) {
    out[[col]] <- as.integer(out[[col]])
  }
  out
}


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

# Render the table as a LaTeX tabular environment (no outer table env / caption)
# and write it to http-verbs.tex.
latex <- function(csv_path = "http-verbs.csv", out_path = "http-verbs.tex") {
  df <- .build_table(csv_path)

  n_cols   <- ncol(df)
  col_spec <- paste0("l", paste(rep("r", n_cols - 1), collapse = ""))
  n_body   <- nrow(df) - 1  # all rows except the Total row

  lines <- c(
    sprintf("\\begin{tabular}{%s}", col_spec),
    "\\hline",
    paste(names(df), collapse = " & ") ,
    "\\hline"
  )
  # Fix: append " \\" to header line
  lines[3] <- paste0(lines[3], " \\\\")

  for (i in seq_len(n_body)) {
    lines <- c(lines,
               paste0(paste(as.character(unlist(df[i, ])), collapse = " & "), " \\\\"))
  }

  lines <- c(lines,
             "\\hline",
             paste0(paste(as.character(unlist(df[nrow(df), ])), collapse = " & "), " \\\\"),
             "\\hline",
             "\\end{tabular}")

  writeLines(lines, out_path)
  invisible(out_path)
}

# Render the table as a Markdown pipe table and write it to http-verbs.md.
markdown <- function(csv_path = "http-verbs.csv", out_path = "http-verbs.md") {
  df <- .build_table(csv_path)

  header    <- paste(names(df), collapse = " | ")
  separator <- paste(rep("---", ncol(df)), collapse = " | ")

  body_lines <- apply(df, 1, function(row) {
    paste(as.character(row), collapse = " | ")
  })

  lines <- c(
    paste0("| ", header, " |"),
    paste0("| ", separator, " |"),
    paste0("| ", body_lines, " |")
  )

  writeLines(lines, out_path)
  invisible(out_path)
}