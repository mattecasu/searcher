package model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@EqualsAndHashCode
public class Product {

  public Product() {
  }

  @Getter
  @Setter
  private String title;
  @Getter
  @Setter
  private String description;
  @Getter
  @Setter
  private String merchant;

}
