package com.spiderclould;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 
 * @author David Wang
 * 2019-11-26
 */
@SpringBootApplication
@ServletComponentScan
public class Application {
	
//	private final static ExecutorService serv = Executors.newFixedThreadPool(10);
//	public static void main(String[] args) {
//		try {
//			
//			Application a = new Application();
//			ServerSocket serverSocket=new ServerSocket(3333);
//			
//			
//			
//			//创建一个客户端对象，这里的作用是用作多线程，必经服务器服务的不是一个客户端
//	        Socket client=null;
//	        boolean flag=true;
//
//	        while(flag){
//	            System.out.println("服务器已启动，等待客户端请求。。。。");
//	            //accept是阻塞式方法，对新手来说这里很有可能出错，下面的注意事项我会说到
//	            client=serverSocket.accept();
//	            //创建一个线程，每个客户端对应一个线程
//	            serv.execute(a.new SubThread(client));
//	        }
//	        client.close();
//	        serverSocket.close();
//	        System.out.println("服务器已关闭。");
//			
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	
//	class SubThread implements Runnable{
//		
//		private Socket client;
//		public SubThread(Socket client){
//			this.client = client;
//		}
//		
//		public void run(){
//		    //run不需要自己去执行，好像是线程器去执行了来着，可以去看api
//		    try {
//		        BufferedReader in=null;
//		        String br=null;
//		        boolean flag=true;
//		        while(flag==true){
//		        //Java流的操作没意见吧
//		            in=new BufferedReader(new InputStreamReader(client.getInputStream()));
//		            br=in.readLine();
//		            System.out.println("++:"+br);
//		            recordMsg(br);//写入到文件
//		        }
//
//		    } catch (IOException e1) {
//		        e1.printStackTrace();
//		    }catch (Exception e) {
//		        System.out.println("error");
//		    }
//
//
//		}
//	}
//	
//	public void recordMsg(String br) throws IOException{
//	    File file=new File("F:\\workspace\\log.txt");
//	    if(!file.exists()){
//	        file.createNewFile();
//	    }
//	    FileWriter writer=new FileWriter(file,true);
//	    writer.write(br+"\r\n");
//	    writer.close();
//
//	}
	
	
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        
        
    }
	

}
