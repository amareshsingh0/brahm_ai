$files = @(
  @{src='c:\desktop\Brahm AI\api\services\query_router.py'; dst='~/books/api/services/query_router.py'; nm='query_router'},
  @{src='c:\desktop\Brahm AI\api\services\prompt_builder.py'; dst='~/books/api/services/prompt_builder.py'; nm='prompt_builder'},
  @{src='c:\desktop\Brahm AI\api\routers\chat.py'; dst='~/books/api/routers/chat.py'; nm='chat_router'},
  @{src='c:\desktop\Brahm AI\api\services\geo_service.py'; dst='~/books/api/services/geo_service.py'; nm='geo_service'},
  @{src='c:\desktop\Brahm AI\api\requirements.txt'; dst='~/books/api/requirements.txt'; nm='requirements'}
)

New-Item -ItemType Directory -Force -Path "c:\desktop\Brahm AI\upload" | Out-Null

foreach ($f in $files) {
  $bytes = [IO.File]::ReadAllBytes($f.src)
  $b64 = [Convert]::ToBase64String($bytes)
  $chunkSize = 2000
  $chunks = [Math]::Ceiling($b64.Length / $chunkSize)
  $nm = $f.nm
  $dst = $f.dst

  $lines = New-Object System.Collections.ArrayList
  [void]$lines.Add("# ===== ${nm}_b64.txt ($chunks chunks) -> $dst =====")

  $i = 0
  while ($i -lt $b64.Length) {
    $chunk = $b64.Substring($i, [Math]::Min($chunkSize, $b64.Length - $i))
    if ($i -eq 0) {
      [void]$lines.Add('printf "%s" "' + $chunk + '" > /tmp/' + $nm + '_b64.txt')
    } else {
      [void]$lines.Add('printf "%s" "' + $chunk + '" >> /tmp/' + $nm + '_b64.txt')
    }
    $i += $chunkSize
  }
  [void]$lines.Add('base64 -d /tmp/' + $nm + '_b64.txt > ' + $dst + ' && echo "OK: ' + $nm + '"')

  $out = "c:\desktop\Brahm AI\upload\${nm}_b64.txt"
  $lines -join "`n" | Set-Content $out -Encoding UTF8
  Write-Host "Written: $out ($chunks chunks)"
}
Write-Host "Done!"
