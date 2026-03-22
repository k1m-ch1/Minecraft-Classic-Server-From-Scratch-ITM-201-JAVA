import gzip
import io

with gzip.open("./chunk_data.gz", 'rb') as file:
    print(file.read(4))

