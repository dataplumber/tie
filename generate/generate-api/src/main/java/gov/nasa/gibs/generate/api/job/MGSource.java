package gov.nasa.gibs.generate.api.job;

public interface MGSource {
   
   public String getProductType();
   public void setProductType(String productType);
   
   public String getProduct();
   public void setProduct(String product);
   
   public String getRepo();
   public void setRepo(String repo);
}
