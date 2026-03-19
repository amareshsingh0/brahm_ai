data = open('api.b64').read()
parts = [data[i:i+15000] for i in range(0, len(data), 15000)]
for i, p in enumerate(parts):
    open(f'api_part{i}.txt', 'w').write(p)
print(f'{len(parts)} parts bane')
