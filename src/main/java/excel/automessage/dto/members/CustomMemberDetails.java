package excel.automessage.dto.members;

import excel.automessage.entity.Members;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
@Slf4j
public class CustomMemberDetails implements UserDetails {

    private final Members members;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        log.info("CustomMemberDetails: getAuthorities");
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add((GrantedAuthority) () -> "ROLE_" + members.getRole().getKey());

        return collection;

    }

    @Override
    public String getPassword() {
        return members.getMemberPassword();
    }

    @Override
    public String getUsername() {
        return members.getMemberId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
