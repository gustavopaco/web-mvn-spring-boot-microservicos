package br.com.curso.webmvnspringbootmicroservicos.security.JWTAlex;

import br.com.curso.webmvnspringbootmicroservicos.model.Usuario;
import br.com.curso.webmvnspringbootmicroservicos.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class JWTTokenAutenticacaoService {

    private final UsuarioRepository usuarioRepository;

    // IMPORTANT: Tempo de validade do JWT - 2 Dias
//    private static final long EXPIRATION_TIME = 172800000;
    private static final long EXPIRATION_TIME = 300000;

    // IMPORTANT: Senha unica que eh adicionada com outros dados para a autenticacao do JWT
    private static final String SECRET = "SenhaExtremamenteSecreta";

    // IMPORTANT: Prefixo que compoe o JWT
    private static final String TOKEN_PREFIX = "Bearer";

    // IMPORTANT: Chave do mapa hash Map<Key,Value> para poder ser chamado e identificar o token
    private static final String HEADER_STRING = "Authorization";

    // IMPORTANT: Gerando Token de autenticacao e adicionando ao cabecalho de resposta Http
    public void addAuthentication(HttpServletRequest request, HttpServletResponse response, User usuario) throws IOException {

        Map<String, Object> map = new HashMap<>();
        map.put("roles", usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        map.put("ip", request.getRemoteAddr());

        // Montagem do token
        String JWT = Jwts.builder()

                // Adiciona o username para ser criptografado
                .setSubject(usuario.getUsername())

                // Definindo a data e hora e expiracao do token
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))

                // Setando URL de onde foi enviado o token
                .setIssuer(request.getRequestURL().toString())

                .addClaims(map)

                // Definindo o algoritmo e Senha unica usada para criptografar os dados
                .signWith(SignatureAlgorithm.HS512, SECRET)

                // Compactacao e algoritmo de geracao de senha
                .compact();

        // Juntando Prefixo + JWT
        String token = TOKEN_PREFIX + " " + JWT;

        // Adicionando Token ao cabecalho Http
        response.addHeader(HEADER_STRING,token);

        // Escrevendo token como resposta no Corpo Http
        response.getWriter().write("{\"Authorization\": \"" + token + "\"}");
    }

    /* IMPORTANT: Retorna o usuario validado com o token ou caso nao seja valido retorna null */
    public Authentication getAuthentication(HttpServletRequest request) {

        // Pega o token enviado no cabecalho Http
        String token = request.getHeader(HEADER_STRING);

        if (token != null) {

            // Descriptografa o token objeto o payload
            Claims claims = Jwts.parser()

                    // Passando o segredo definido para descriptografia
                    .setSigningKey(SECRET)

                    //  Removendo o Bearer do inicio do token e descriptografando
                    .parseClaimsJws(token.substring("Bearer ".length()))

                    //  Descriptografando o token e obtendo o Payload(Subject) das partes {Header, Payload, Signature}
                    .getBody();

            String username = claims.getSubject();
            List<?> list = (List<?>) claims.get("roles");
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (Object role : list) {
                authorities.add(new SimpleGrantedAuthority(role.toString()));
            }

            if (username != null) {
                return new UsernamePasswordAuthenticationToken(username, null, authorities);
            }

            // IMPORTANT: Outro jeito de validar usuario realizando consulta ao banco apos validar o Token possuindo atributo username
            // if (username != null) {
                // Pesquisa o usuario no Banco de dados a partir do username
                // Usuario usuario = usuarioRepository.findUsuarioByLogin(username);
                //
                // if (usuario != null) {
                //  Retorna usuario com seus dados criptografados
                        // return new UsernamePasswordAuthenticationToken(usuario.getUsername(), usuario.getPassword(), usuario.getAuthorities());
                // }
            // }
        }

        // Usuario nao autorizado
        return null;
    }
}