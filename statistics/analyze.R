
#EMB,NAME,TYPE,LANGUAGE,RUNTIME,BUILD,FILES,LOCS,DATABASE,LICENSE,ENDPOINTS,AUTHENTICATION,URL
DATA_FILE <- "./data.csv"

UNDEFINED <- "UNDEFINED"

handleMultiValues <- function(s){
  return(gsub(";", ", ", s))
}

### return a boolean vector, where each position in respect to x is true if that element appear in y
areInTheSubset <- function(x,y){

  ### first consider vector with all FALSE
  result <- x!=x
  for(k in y){
    result <- result | x==k
  }
  return(result)
}


markdown <- function (){

  dt <- read.csv(DATA_FILE,header=T)

  dt <- dt[order(dt$TYPE, dt$NAME, -dt$LOCS, dt$LANGUAGE),]
  # skip industrial APIs that are not stored in EMB
  dt <- dt[dt$EMB==TRUE,]

  TABLE <- "./table_wfd.md"
  unlink(TABLE)
  sink(TABLE, append = TRUE, split = TRUE)

  #EMB,NAME,TYPE,LANGUAGE,RUNTIME,BUILD,FILES,LOCS,DATABASE,LICENSE,ENDPOINTS,AUTHENTICATION,URL
  cat("|Type|Name|#LOCs|#SourceFiles|#Endpoints|Language(s)|Runtime|Build Tool|Database(s)|Authentication|\n")
  ## Note: the ":" are used for alignment of the columns
  cat("|----|----|----:|-----------:|---------:|-----------|-------|----------|-----------|:------------:|\n")

  for (i in 1:nrow(dt)){

    row <- dt[i,]
    cat("|")
    cat(row$TYPE,"|",sep="")
    cat("__",row$NAME,"__|",sep="")
    cat(row$LOCS,"|",sep="")
    cat(row$FILES,"|",sep="")
    cat(row$ENDPOINTS,"|",sep="")
    cat(handleMultiValues(row$LANGUAGE),"|",sep="")
    cat(row$RUNTIME,"|",sep="")
    cat(row$BUILD,"|",sep="")
    cat(handleMultiValues(row$DATABASE),"|",sep="")

    if(row$AUTHENTICATION){
        cat("&check;")
    }
    cat("|")


    cat("\n")
  }

  sink()
}


latex <- function(TABLE,SUTS,auth, databases){

  # TODO what columns to include further could be passed as boolean selection.
  # will implement when needed

  dt <- read.csv(DATA_FILE,header=T)
  dt <- dt[areInTheSubset(dt$NAME,SUTS),]
  dt <- dt[order(dt$NAME),]

  unlink(TABLE)
  sink(TABLE, append = TRUE, split = TRUE)

  cat("\\begin{tabular}{l rrr")
  if(auth) cat("r")
  if(databases) cat("r")
  cat("}\\\\ \n")
  cat("\\toprule \n")
  cat("SUT & \\#SourceFiles & \\#LOCs & \\#Endpoints ")
  if(auth) cat("& Auth")
  if(databases) cat("& Databases")
  cat("\\\\ \n")
  cat("\\midrule \n")

  for (i in 1:nrow(dt)){

    row <- dt[i,]
    cat("\\emph{",row$NAME,"}",sep="")

    cat(" & ", row$FILES)
    cat(" & ", row$LOCS)
    cat(" & ", row$ENDPOINTS)

    if(auth){
      cat(" & ")
      if(row$AUTHENTICATION){
          cat("\\checkmark")
      }
    }
    if(databases){
      cat(" & ",row$DATABASE)
    }

    cat(" \\\\ \n")
  }

  cat("\\midrule \n")
  cat("Total",nrow(dt))
  cat(" & ")
  cat(sum(dt$FILES))
  cat(" & ")
  cat(sum(dt$LOCS))
  cat(" & ")
  cat(sum(dt$ENDPOINTS))
  if(auth){
    cat(" & ")
    cat(sum(dt$AUTHENTICATION))
  }
  if(databases){
    cat(" & ")
    cat(sum(!is.na(dt$DATABASE) & trimws(dt$DATABASE) != ""))
  }

  cat(" \\\\ \n")

  cat("\\bottomrule \n")
  cat("\\end{tabular} \n")

  sink()
}


oldLatexTable <- function(){

  dt <- read.csv(DATA_FILE,header=T)

  dt <- dt[order(dt$TYPE, dt$LANGUAGE, -dt$LOCS, dt$NAME),]

  TABLE <- "./old_statistics_table_emb.tex"
  unlink(TABLE)
  sink(TABLE, append = TRUE, split = TRUE)

  cat("\\begin{tabular}{lll rr ll}\\\\ \n")
  cat("\\toprule \n")
  cat("SUT & Type & Language & \\#Files & \\#LOCs & Database & URL \\\\ \n")
  cat("\\midrule \n")

  for (i in 1:nrow(dt)){

    row <- dt[i,]
    cat("\\emph{",row$NAME,"}",sep="")

    cat(" & ", row$TYPE)
    cat(" & ", row$LANGUAGE)
    cat(" & ", row$FILES)
    cat(" & ", row$LOCS)

    databases = gsub(";", ", ", row$DATABASE)
    cat(" & ", databases)

    url <- row$URL
    if(url == "UNDEFINED"){
      cat(" & - ")
    } else {
      cat(" & \\url{", url,"}",sep="")
    }

    cat(" \\\\ \n")
  }

  cat("\\midrule \n")
  cat("Total",nrow(dt))
  cat(" & & & ")
  cat(sum(dt$FILES))
  cat(" & ")
  cat(sum(dt$LOCS))
  cat(" & ")
  cat(length(dt$DATABASE[dt$DATABASE != ""]))
  cat(" & ")
  cat(" \\\\ \n")

  cat("\\bottomrule \n")
  cat("\\end{tabular} \n")

  sink()
}