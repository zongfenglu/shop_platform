$files = @(
  'd:\owner\shop_platform\shop-client-api\src\main\java\com\shopplatform\clientapi\controller\ConsumerGroupController.java',
  'd:\owner\shop_platform\shop-client-api\src\main\java\com\shopplatform\clientapi\controller\ConsumerBargainController.java'
)
$gbk = [System.Text.Encoding]::GetEncoding(936)
$utf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($f in $files) {
  if (Test-Path $f) {
    $content = [System.IO.File]::ReadAllText($f, $gbk)
    [System.IO.File]::WriteAllText($f, $content, $utf8)
    Write-Output ("converted " + $f)
  } else {
    Write-Output ("missing " + $f)
  }
}
