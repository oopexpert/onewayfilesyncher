# Example
```
	public static void main(String[] args) {
		
		File source = new File("C:\\Daten");
		File target = new File("C:\\DatenSicherung");
		
		new OneWayFileSyncher(source, target).start();
		
	}
```